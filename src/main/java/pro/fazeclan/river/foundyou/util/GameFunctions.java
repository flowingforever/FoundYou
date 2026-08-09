package pro.fazeclan.river.foundyou.util;

import net.kyori.adventure.text.format.NamedTextColor;
import org.alexdev.unlimitednametags.api.UNTPaperAPI;
import org.alexdev.unlimitednametags.config.Settings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GamePlayerDeathEvent;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.ConditionUtil;
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
        jarona.getServer().getPluginManager().callEvent(new GamePlayerDeathEvent(player, RoleUtil.getRoleOrThrow(player)));
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

    public static void addPlayer(Player player, Role role, Location location) {
        var nametagManager = UNTPaperAPI.getInstance();

        RoleUtil.removeRoles(player);
        RoleUtil.assignRole(player, role, location);

        String color;
        if (role.getFaction().equals(Faction.HUNTERS)) {
            color = "red";
        } else {
            color = "green";
        }
        nametagManager.modifyNametagProperty(player, original -> {
            var groups = new ArrayList<>(original.displayGroups());
            groups.removeFirst();
            groups.add(Settings.DisplayGroup
                    .builder()
                    .line("<" + color + ">" + player.getName() + "</" + color + ">")
                    .build());
            return original.withDisplayGroups(groups);
        });
        for (var p : player.getWorld().getPlayers()) {
            NametagUtil.hidePlayerNametag(p, player);
        }
        NametagUtil.hidePlayerNametagWithGlowToAll(player, NamedTextColor.NAMES.value(color));
        FoundYou.getInstance()
                .getServer()
                .getPluginManager()
                .callEvent(new GameAddPlayerEvent(player, role));
    }

    public static void removePlayer(Player player) {
        var manager = UNTPaperAPI.getInstance();
        RoleUtil.removeRoles(player);
        manager.removeNametagOverride(player);
        NametagUtil.showPlayerNametagToAll(player);
    }

    public static void addGameTime(World world, long ticks) {
        UUID gameUUID;
        try {
            gameUUID = UUID.fromString(world.getKey().getKey());
        } catch (Exception e) {
            return;
        }
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
        long max = world.getPersistentDataContainer().getOrDefault(FoundYou.getKey("max_game_length"), PersistentDataType.LONG, -1L);
        long newTime = world.getPersistentDataContainer().getOrDefault(
                FoundYou.getKey("game_length"),
                PersistentDataType.INTEGER,
                900
        ) + ticks;
        if (max != -1) {
            newTime = Math.min(newTime, max);
        }
        world.getPersistentDataContainer().set(
                FoundYou.getKey("game_length"),
                PersistentDataType.INTEGER,
                (int) newTime
        );
    }

}
