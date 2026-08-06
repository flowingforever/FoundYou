package pro.fazeclan.river.foundyou.util;

import de.tr7zw.nbtapi.NBT;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GamePlayerDeathEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.GameUtil;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

public class RoleUtil {

    public static void assignRole(Player player, Role role) {
        player.getScoreboardTags().add("foundyou_" + role.getId());
        player.getScoreboardTags().add("foundyoufaction_" + role.getFaction().toString().toLowerCase());
        GameUtil.resetPlayer(player, GameMode.ADVENTURE);
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.WATER_BREATHING,
                        PotionEffect.INFINITE_DURATION,
                        0, true, false, true
                )
        );
        NBT.modify(player, nbt -> {
            try {
                nbt.mergeCompound(NBT.parseNBT(role.getItems()));
            } catch (Exception ignored) {}
        });
        player.updateInventory();
        FoundYou.getInstance()
                .getServer()
                .getPluginManager()
                .callEvent(new GameAddPlayerEvent(player, role));
    }

    public static void assignRole(Player player, Role role, Location teleport) {
        assignRole(player, role);
        player.teleport(teleport);
    }

    public static void removeRoles(Player player) {
        var role = getRole(player);
        if (role.isPresent()) {
            player.getScoreboardTags().removeIf(tag -> tag.startsWith("foundyou_"));
            removeFaction(player);
            FoundYou.getInstance()
                    .getServer()
                    .getPluginManager()
                    .callEvent(new GameRemovePlayerEvent(player, role.get()));
        }
    }

    public static boolean isRole(Player player, Role role) {
        return isRole(player, role.getId());
    }

    public static boolean isRole(Player player, String id) {
        return player.getScoreboardTags().contains("foundyou_" + id);
    }

    public static Role getRoleOrThrow(Player player) {
        return getRole(player).get();
    }

    public static Optional<Role> getRole(Player player) {
        var manager = FoundYou.getInstance().getRoleManager();
        return player.getScoreboardTags()
                .stream()
                .filter(tag -> tag.startsWith("foundyou_"))
                .map(tag -> manager.getRole(tag.replace("foundyou_", "")))
                .findFirst();
    }

    public static Optional<Faction> getFaction(Player player) {
        return player.getScoreboardTags()
                .stream()
                .filter(tag -> tag.startsWith("foundyoufaction_"))
                .map(tag -> Faction.valueOf(tag.replace("foundyoufaction_", "").toUpperCase()))
                .findFirst();
    }

    public static Faction getFactionElseThrow(Player player) {
        return getFaction(player).get();
    }

    public static void removeFaction(Player player) {
        player.getScoreboardTags().removeIf(tag -> tag.startsWith("foundyoufaction_"));
    }

    public static boolean isSameFaction(Player p1, Player p2) {
        return getFactionElseThrow(p1).equals(getFactionElseThrow(p2));
    }

    public static void eliminatePlayer(Player player) {
        var world = player.getWorld();
        var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));
        var jarona = Jarona.getInstance();
        var plugin = FoundYou.getInstance();
        var manager = jarona.getConditionManager();
        var gameUUID = UUID.fromString(world.getKey().getKey());
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
        var condition = manager.getGameConditions(gameUUID)
                .getOrCreate(
                        "game_" + gameUUID,
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK
                        )
                );
        var time = config.getInt("additional-time", 900);
        condition.setDuration(condition.getDuration() + time);
        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = tc.getDuration();
            return "<red><b>" + TimeUtil.ticksIntoReadableFormat((int) duration) + "</b></red>";
        });

        world.getPersistentDataContainer().set(
                FoundYou.getKey("game_length"),
                PersistentDataType.INTEGER,
                world.getPersistentDataContainer().getOrDefault(
                        FoundYou.getKey("game_length"),
                        PersistentDataType.INTEGER,
                        900
                ) + time
        );

        var svcPlugin = plugin.getVoicechatPlugin();
        if (svcPlugin != null) {
            svcPlugin.addSpectator(player);
        }
    }

}
