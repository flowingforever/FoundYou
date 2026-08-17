package pro.fazeclan.river.foundyou.util;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.alexdev.unlimitednametags.api.UNTPaperAPI;
import org.alexdev.unlimitednametags.config.Settings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GamePlayerDeathEvent;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.game.GameValues;
import pro.fazeclan.river.jarona.util.ConditionUtil;
import pro.fazeclan.river.jarona.util.GameUtil;
import pro.fazeclan.river.jarona.util.NametagUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class GameFunctions {

    public static void eliminatePlayer(Player player) {
        var world = player.getWorld();
        var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));
        var jarona = Jarona.getInstance();
        var plugin = FoundYou.getInstance();
        jarona.getServer().getPluginManager().callEvent(new GamePlayerDeathEvent(player, RoleUtil.getRole(player)));
        player.setGameMode(GameMode.SPECTATOR);
        world.playSound(
                player.getLocation(),
                "minecraft:block.beacon.deactivate",
                SoundCategory.PLAYERS,
                2f,
                0.5f
        );

        // todo: summon corpse

        // add more time when player dies
        var time = config.getInt("additional-time", 900);
        addGameTime(world, time);

        var svcPlugin = plugin.getVoicechatPlugin();
        if (svcPlugin != null) {
            svcPlugin.addSpectator(player);
        }
    }

    public static void addPlayer(Player player, Role role, GameValues values, Location location) {
        var nametagManager = UNTPaperAPI.getInstance();
        var mm = MiniMessage.miniMessage();

        player.getScoreboardTags().removeIf(tag -> tag.startsWith("foundyoufaction"));
        RoleUtil.removeRoles(player);
        RoleUtil.assignRole(player, role, location);

        String text;
        NamedTextColor color;
        Title title;
        if (role.getFaction().equals(Faction.HUNTERS)) {
            text = "<red><b>HUNTER</b></red>";
            color = NamedTextColor.RED;
            title = Title.title(
                    mm.deserialize("<yellow>You're a <red>Hunter!"),
                    mm.deserialize("<red>Catch and kill all runners to win.")
            );
            player.getScoreboardTags().add("foundyoufaction_hunters");
            values.setValue("tablist_name_" + player.getUniqueId(), "<red>\uD83E\uDE93 %jarona_nickname%</red>");
        } else {
            text = "<green><b>RUNNER</b></green>";
            color = NamedTextColor.GREEN;
            title = Title.title(
                    mm.deserialize("<yellow>You're a <green>Runner!"),
                    mm.deserialize("<green>Avoid being killed by hunters to win!")
            );
            player.getScoreboardTags().add("foundyoufaction_runners");
            values.setValue("tablist_name_" + player.getUniqueId(), "<green>❤ %jarona_nickname%</green>");
        }
        player.setSaturation(0f);
        player.showTitle(title);
        nametagManager.modifyNametagProperty(player, original -> {
            var groups = new ArrayList<>(original.displayGroups());
            groups.add(Settings.DisplayGroup
                    .builder()
                    .line(text)
                    .yOffset(0.25f)
                    .build());
            return original.withDisplayGroups(groups);
        });
        NametagUtil.hidePlayerNametagWithGlowToAll(player, color);
        FoundYou.getInstance()
                .getServer()
                .getPluginManager()
                .callEvent(new GameAddPlayerEvent(player, role));
    }

    public static void removePlayer(Player player) {
        var manager = UNTPaperAPI.getInstance();
        RoleUtil.removeRoles(player);
        manager.removeNametagOverride(player);
        manager.setNametagSeeThrough(player, true);
        NametagUtil.showPlayerNametagToAll(player);
        player.getScoreboardTags().removeIf(tag -> tag.startsWith("foundyoufaction"));
        ConditionUtil.getPlayerConditions(player).clear();
    }

    public static void addGameTime(World world, long ticks) {
        UUID gameUUID;
        try {
            gameUUID = UUID.fromString(world.getKey().getKey());
        } catch (Exception e) {
            return;
        }
        var game = GameUtil.getGame(world);
        var condition = ConditionUtil.getWorldConditions(world)
                .getOrCreate(
                        "game_" + gameUUID,
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK
                        )
                );
        condition.setDuration(condition.getDuration() + ticks);
        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = tc.getDuration();
            return "<red><b>" + TimeUtil.ticksIntoReadableFormat((int) duration) + "</b></red>";
        });
        var gameValues = game.getGameValues(world.getUID());
        long max = gameValues.getValue("max_game_length", -1L);
        long newTime = gameValues.getValue("game_length", 900L) + ticks;
        if (max != -1) {
            newTime = Math.min(newTime, max);
        }
        gameValues.setValue("game_length", newTime);
    }

}
