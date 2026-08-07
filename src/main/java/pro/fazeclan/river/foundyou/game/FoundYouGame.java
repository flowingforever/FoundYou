package pro.fazeclan.river.foundyou.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.alexdev.unlimitednametags.api.UNTPaperAPI;
import org.alexdev.unlimitednametags.api.UntNametagManagerPaper;
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
import pro.fazeclan.river.foundyou.event.GracePeriodOverEvent;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.foundyou.util.GameFunctions;
import pro.fazeclan.river.foundyou.util.RoleUtil;
import pro.fazeclan.river.foundyou.util.TimeUtil;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.game.Game;
import pro.fazeclan.river.jarona.util.GameUtil;
import pro.fazeclan.river.jarona.util.NametagUtil;
import pro.fazeclan.river.jarona.util.WorldlessLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FoundYouGame extends Game {
    public FoundYouGame() {
        super(
                "<red>Found You!</red>",
                FoundYou.getKey("game"),
                true,
                true,
                2
        );
    }

    @Override
    public void init(World world, List<Player> players) {
        var plugin = FoundYou.getInstance();
        var jarona = Jarona.getInstance();

        var conditionManager = jarona.getConditionManager();
        var nametagManager = UNTPaperAPI.getInstance();

        var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));

        var miniMessage = MiniMessage.miniMessage();

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

        assignRoles(hunters, Faction.HUNTERS, hunterSpawn);
        assignRoles(runners, Faction.RUNNERS, runnerSpawn);

        // game prep
        for (Player player : players) {
            var role = RoleUtil.getRoleOrThrow(player);

            Title title = Title.title(Component.empty(), Component.empty());
            switch (role.getFaction()) {
                case RUNNERS -> {
                    title = Title.title(
                            miniMessage.deserialize("<yellow>You're a <green>Runner!"),
                            miniMessage.deserialize("<green>Avoid being killed by hunters to win!")
                    );
                    nametagManager.setForcedNametag(player, miniMessage.deserialize("<green>" + player.getName() + "</green>"));
                    nametagManager.setNametagSeeThrough(player, false);
                    NametagUtil.hidePlayerNametagWithGlowToAll(player, NamedTextColor.GREEN);
                }
                case HUNTERS -> {
                    title = Title.title(
                            miniMessage.deserialize("<yellow>You're a <red>Hunter!"),
                            miniMessage.deserialize("<red>Catch and kill all runners to win.")
                    );
                    nametagManager.setForcedNametag(player, miniMessage.deserialize("<red>" + player.getName() + "</red>"));
                    ((UntNametagManagerPaper) UNTPaperAPI.getInstance().nametagManager()).hideOtherNametags(player);
                    nametagManager.setNametagSeeThrough(player, true);
                    NametagUtil.hidePlayerNametagWithGlowToAll(player, NamedTextColor.RED);

                    var svcPlugin = plugin.getVoicechatPlugin();
                    if (svcPlugin != null) {
                        svcPlugin.addHunter(player);
                    }

                    if (hunterCount == 1) {
                        player.getEquipment().getBoots().editMeta(meta -> meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(
                                FoundYou.getKey("max_health"),
                                10.0,
                                AttributeModifier.Operation.ADD_NUMBER,
                                EquipmentSlotGroup.FEET
                        )));
                    }
                }
            }
            player.showTitle(title);

        }

        var graceLength = config.getInt("grace-length");
        var gameLength = config.getInt("initial-time");
        world.getPersistentDataContainer().set(
                FoundYou.getKey("grace_length"),
                PersistentDataType.INTEGER,
                graceLength
        );
        world.getPersistentDataContainer().set(
                FoundYou.getKey("game_length"),
                PersistentDataType.INTEGER,
                gameLength
        );

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

        var worldPDC = world.getPersistentDataContainer();
        var minimessage = MiniMessage.miniMessage();

        var graceLength = worldPDC.get(FoundYou.getKey("grace_length"), PersistentDataType.INTEGER);
        var gameLength = worldPDC.get(FoundYou.getKey("game_length"), PersistentDataType.INTEGER);

        var totalGameLength = graceLength + gameLength;

        worldPDC.set(
                FoundYou.getKey("tick"),
                PersistentDataType.INTEGER,
                getCurrentGameTick(world) + 1
        );

        if (!areRunnersAlive(players) || !areHuntersAlive(players)) {
            GameUtil.endGame(world);
        }

        if (getCurrentGameTick(world) == 100) {
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

        if (getCurrentGameTick(world) == graceLength) {
            var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));

            for (Player player : players) {
                player.sendMessage(minimessage.deserialize(
                        "<yellow>Hunters have entered the map, good luck!</yellow>"
                ));

                if (RoleUtil.getFactionElseThrow(player) == Faction.HUNTERS) {
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

        if (getCurrentGameTick(world) == totalGameLength - 200) {
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

        if (getCurrentGameTick(world) >= totalGameLength) {
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

        var nametagManager = UNTPaperAPI.getInstance();
        var conditionManager = jarona.getConditionManager();
        conditionManager.getGameConditions(gameUUID).remove("game_" + gameUUID);

        for (Player player : players) {
            nametagManager.clearForcedNametag(player);
            nametagManager.setNametagSeeThrough(player, true);
            NametagUtil.showPlayerNametagToAll(player);
            RoleUtil.removeRoles(player);

            var svcPlugin = plugin.getVoicechatPlugin();
            if (svcPlugin != null) {
                svcPlugin.removePlayer(player);
            }

            if (runnersWon) {
                player.showTitle(
                        Title.title(
                                miniMessage.deserialize("<green>Runners"),
                                miniMessage.deserialize("win!")
                        )
                );
            } else {
                player.showTitle(
                        Title.title(
                                miniMessage.deserialize("<red>Hunters"),
                                miniMessage.deserialize("win!")
                        )
                );
            }
        }
    }

    private int getCurrentGameTick(World world) {
        return world.getPersistentDataContainer().getOrDefault(FoundYou.getKey("tick"), PersistentDataType.INTEGER, 0);
    }

    private boolean isFactionAlive(List<Player> players, Faction faction) {
        return players.stream()
                .filter(player -> !player.getGameMode().isInvulnerable())
                .filter(player -> RoleUtil.getFaction(player).isPresent())
                .anyMatch(player -> RoleUtil.getFactionElseThrow(player).equals(faction));
    }

    private boolean areRunnersAlive(List<Player> players) {
        return isFactionAlive(players, Faction.RUNNERS);
    }

    private boolean areHuntersAlive(List<Player> players) {
        return isFactionAlive(players, Faction.HUNTERS);
    }

    private void assignRoles(List<Player> players, Faction faction, Location location) {
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
                    GameFunctions.addPlayer(player, limitedRoles.getFirst(), location);
                    limitedRoles.removeFirst();
                    chance /= dividend;
                    continue;
                }

                chance = 0f;
            }

            if (index + 1 > size) {
                index = 0;
            }

            GameFunctions.addPlayer(player, unlimitedRoles.get(index), location);
            index++;
        }
    }
}
