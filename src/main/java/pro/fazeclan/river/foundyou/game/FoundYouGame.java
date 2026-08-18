package pro.fazeclan.river.foundyou.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.alexdev.unlimitednametags.api.UNTPaperAPI;
import org.alexdev.unlimitednametags.api.UntNametagManagerPaper;
import org.alexdev.unlimitednametags.config.Settings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.GamePlayerLoseEvent;
import pro.fazeclan.river.foundyou.event.GamePlayerWinEvent;
import pro.fazeclan.river.foundyou.event.GracePeriodOverEvent;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.foundyou.util.GameFunctions;
import pro.fazeclan.river.foundyou.util.RoleUtil;
import pro.fazeclan.river.foundyou.util.TimeUtil;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.game.Game;
import pro.fazeclan.river.jarona.game.GameValues;
import pro.fazeclan.river.jarona.game.GameWithMap;
import pro.fazeclan.river.jarona.stats.AchievementDefinition;
import pro.fazeclan.river.jarona.stats.StatisticDefinition;
import pro.fazeclan.river.jarona.util.GameUtil;
import pro.fazeclan.river.jarona.util.NametagUtil;
import pro.fazeclan.river.jarona.util.WorldlessLocation;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FoundYouGame extends GameWithMap {
    public FoundYouGame() {
        super(
                "<red>Found You!</red>",
                FoundYou.getKey("game"),
                2
        );
    }

    @Override
    public void init(World world, List<Player> players) {
        var jarona = Jarona.getInstance();
        var gameValues = getGameValues(world.getUID());
        var statManager = jarona.getStatisticManager();

        var conditionManager = jarona.getConditionManager();

        var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));

        // okay proper starting neow

        var runnerSpawn = WorldlessLocation.deserialize("spawn", config).toLocation(world);
        var hunterSpawn = WorldlessLocation.deserialize("hunter-spawn", config).toLocation(world);

        // selecting hunters
        var hunters = new ArrayList<Player>();

        var hunterCount = 1 + Math.floor(players.size() / 7.5);

        var runners = new ArrayList<>(players);
        Collections.shuffle(runners);

        for (int i = 0; i < hunterCount; i++) {
            var hunter = runners.getFirst();
            hunters.add(hunter);
            runners.remove(hunter);
        }

        assignRoles(hunters, Faction.HUNTERS, gameValues, hunterSpawn);
        assignRoles(runners, Faction.RUNNERS, gameValues, runnerSpawn);

        // game prep
        for (Player player : players) {
            var role = RoleUtil.getRole(player);
            statManager.incrementStatistic(
                    player.getUniqueId(),
                    getKey(),
                    FoundYou.getKey("games_played"),
                    1
            );

            if (role.getFaction() == Faction.HUNTERS) {
                statManager.incrementStatistic(
                        player.getUniqueId(),
                        getKey(),
                        FoundYou.getKey("games_played_as_hunter"),
                        1
                );

                if (hunterCount == 1) {
                    player.getEquipment().getBoots().editMeta(meta -> meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(
                            FoundYou.getKey("max_health"),
                            10.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.FEET
                    )));
                }
            } else {
                statManager.incrementStatistic(
                        player.getUniqueId(),
                        getKey(),
                        FoundYou.getKey("games_played_as_runner"),
                        1
                );
            }

        }

        long graceLength = gameValues.setValue("grace_length", config.getLong("grace-length"));
        long gameLength = gameValues.setValue("game_length", config.getLong("initial-time"));
        gameValues.setValue("max_game_length", config.getLong("max-game-length", -1));

        var gameUUID = UUID.fromString(world.getKey().getKey());

        // visualize game length
        var timedCondition = conditionManager
                .getGameConditions(gameUUID)
                .getOrCreate(
                        "game_" + gameUUID,
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK
                        )
                );

        timedCondition.setHud(condition -> {
            var tc = (TimedCondition) condition;
            long duration;
            if (timedCondition.getDuration() >= gameLength) {
                duration = (tc.getDuration() - gameLength);
                return "<gold><b>" + TimeUtil.ticksIntoReadableFormat((int) duration) + "</b></gold>";
            } else {
                duration = tc.getDuration();
                return "<red><b>" + TimeUtil.ticksIntoReadableFormat((int) duration) + "</b></red>";
            }
        });

        timedCondition.setHudCondition(_ -> true);
        timedCondition.setDuration(gameLength + graceLength);

        // tab stuffs
        var pdc = world.getPersistentDataContainer();
        pdc.set(Jarona.getKey("tablist_header"), PersistentDataType.STRING,
                "<newline><red>Found You!</red><newline>");

    }

    @Override
    public void tick(World world, List<Player> players) {

        var minimessage = MiniMessage.miniMessage();

        var gameValues = getGameValues(world.getUID());
        long graceLength = gameValues.getValue("grace_length", 100L);
        long gameLength = gameValues.getValue("game_length", 100L);

        var totalGameLength = graceLength + gameLength;

        long tick = gameValues.setValue("tick", getCurrentGameTick(world) + 1L);

        if (!areRunnersAlive(players) || !areHuntersAlive(players)) {
            GameUtil.endGame(world);
        }

        if (tick == 100) {
            var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));

            for (Player player : players) {
                player.showTitle(
                        Title.title(
                                minimessage.deserialize(config.getString("name")),
                                minimessage.deserialize(config.getString("credits"))
                        )
                );
            }
        }

        if (tick == graceLength) {
            var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));

            for (Player player : players) {
                player.sendMessage(minimessage.deserialize(
                        "<yellow>Hunters have entered the map, good luck!</yellow>"
                ));

                if (RoleUtil.getFaction(player) == Faction.HUNTERS) {
                    player.teleport(WorldlessLocation.deserialize("spawn", config).toLocation(world));
                    player.addPotionEffect(
                            new PotionEffect(
                                    PotionEffectType.GLOWING,
                                    PotionEffect.INFINITE_DURATION,
                                    0, true, false
                            )
                    );
                }
            }

            FoundYou.getInstance().getServer().getPluginManager().callEvent(new GracePeriodOverEvent(world, players));
        }

        if (tick == totalGameLength - 200) {
            for (Player player : players) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.GLOWING,
                        200,
                        0,
                        true,
                        false,
                        true
                ));
                player.sendMessage(minimessage.deserialize(
                        "<yellow>Final stretch! 10 seconds remain!</yellow>"
                ));
            }
        }

        if (tick >= totalGameLength) {
            GameUtil.endGame(world);
        }

    }

    @Override
    public void end(World world, List<Player> players) {
        var jarona = Jarona.getInstance();
        var plugin = FoundYou.getInstance();
        var gameUUID = UUID.fromString(world.getKey().getKey());
        var miniMessage = MiniMessage.miniMessage();

        boolean runnersWon = areRunnersAlive(players);

        var conditionManager = jarona.getConditionManager();
        conditionManager.getGameConditions(gameUUID).remove("game_" + gameUUID);

        var pluginManager = jarona.getServer().getPluginManager();
        for (Player player : players) {
            var svcPlugin = plugin.getVoicechatPlugin();
            if (svcPlugin != null) {
                svcPlugin.removePlayer(player);
            }

            if (runnersWon) {
                player.showTitle(
                        Title.title(
                                miniMessage.deserialize("<green><b>Runners</b></green>"),
                                miniMessage.deserialize("win!")
                        )
                );

                if (RoleUtil.getFaction(player) == Faction.RUNNERS) {
                    pluginManager.callEvent(new GamePlayerWinEvent(player, Faction.RUNNERS));
                } else {
                    pluginManager.callEvent(new GamePlayerLoseEvent(player, Faction.HUNTERS));
                }
            } else {
                player.showTitle(
                        Title.title(
                                miniMessage.deserialize("<red><b>Hunters</b></red>"),
                                miniMessage.deserialize("win!")
                        )
                );

                if (RoleUtil.getFaction(player) == Faction.HUNTERS) {
                    pluginManager.callEvent(new GamePlayerWinEvent(player, Faction.HUNTERS));
                } else {
                    pluginManager.callEvent(new GamePlayerLoseEvent(player, Faction.RUNNERS));
                }
            }

            GameFunctions.removePlayer(player);

        }
    }

    private long getCurrentGameTick(World world) {
        return getGameValues(world.getUID()).getValue("tick", 0L);
    }

    private List<Player> getPlayers(List<Player> players, Faction faction) {
        return players.stream()
                .filter(player -> faction.equals(RoleUtil.getFaction(player)))
                .toList();
    }

    private List<Player> getAlivePlayers(List<Player> players, Faction faction) {
        return players.stream()
                .filter(player -> !player.getGameMode().isInvulnerable())
                .filter(player -> faction.equals(RoleUtil.getFaction(player)))
                .toList();
    }

    private boolean isFactionAlive(List<Player> players, Faction faction) {
        return players.stream()
                .filter(player -> !player.getGameMode().isInvulnerable())
                .anyMatch(player -> faction.equals(RoleUtil.getFaction(player)));
    }

    private boolean areRunnersAlive(List<Player> players) {
        return isFactionAlive(players, Faction.RUNNERS);
    }

    private boolean areHuntersAlive(List<Player> players) {
        return isFactionAlive(players, Faction.HUNTERS);
    }

    private void assignRoles(List<Player> players, Faction faction, GameValues values, Location location) {
        var manager = FoundYou.getInstance().getRoleManager();

        var limitedRoles = new ArrayList<Role>();
        for (var role : manager.getLimitedRoles(faction)) {
            for (int i = 0; i < role.getMaxPlayers(); i++) {
                limitedRoles.add(role);
            }
        }
        Collections.shuffle(limitedRoles);

        var unlimitedRoles = new ArrayList<>(manager.getUnlimitedRoles(faction));
        Collections.shuffle(unlimitedRoles);
        int size = unlimitedRoles.size();
        int index = 0;

        float dividend = (float) FoundYou.getInstance().getConfig().getDouble("role-chance-dividend", 2.0);
        float chance = 1.0f;
        for (var player : players) {
            if (chance > 0f) {
                if (ThreadLocalRandom.current().nextFloat() <= chance && !limitedRoles.isEmpty()) {
                    GameFunctions.addPlayer(player, limitedRoles.getFirst(), values, location);
                    limitedRoles.removeFirst();
                    chance /= dividend;
                    continue;
                }

                chance = 0f;
            }

            if (index + 1 > size) {
                index = 0;
            }

            GameFunctions.addPlayer(player, unlimitedRoles.get(index), values, location);
            index++;
        }
    }

    @Override
    public List<StatisticDefinition> getStatDefinitions() {
        return List.of(
                new StatisticDefinition(FoundYou.getKey("kills_as_hunter"), "<red>Kills as Hunter</red>", 0),
                new StatisticDefinition(FoundYou.getKey("deaths_as_hunter"), "<red>Deaths as Hunter</red>", 0),
                new StatisticDefinition(FoundYou.getKey("wins_as_hunter"), "<red>Wins as Hunter</red>", 0),
                new StatisticDefinition(FoundYou.getKey("games_played_as_hunter"), "<red>Games Played as Hunter</red>", 0),
                new StatisticDefinition(FoundYou.getKey("kills_as_runner"), "<green>Kills as Runner</green>", 0),
                new StatisticDefinition(FoundYou.getKey("deaths_as_runner"), "<green>Deaths as Runner</green>", 0),
                new StatisticDefinition(FoundYou.getKey("wins_as_runner"), "<green>Wins as Runner</green>", 0),
                new StatisticDefinition(FoundYou.getKey("games_played_as_runner"), "<green>Games Played as Runner</green>", 0),
                new StatisticDefinition(FoundYou.getKey("experience"), "<aqua>Experience</aqua>", 0),
                new StatisticDefinition(FoundYou.getKey("games_played"), "<aqua>Games Played</aqua>", 0)
        );
    }

    @Override
    public List<AchievementDefinition> getAchievementDefinitions() {
        return List.of(
                new AchievementDefinition(FoundYou.getKey("hunter_win"), "<red>Slaughter</red>", 0, 1),
                new AchievementDefinition(FoundYou.getKey("runner_win"), "<green>Survivor</green>", 0, 1)
        );
    }
}
