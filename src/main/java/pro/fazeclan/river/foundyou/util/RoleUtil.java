package pro.fazeclan.river.foundyou.util;

import de.tr7zw.nbtapi.NBT;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.foundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.jarona.util.GameUtil;

import java.util.Optional;

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
        FoundYou.getInstance()
                .getServer()
                .getPluginManager()
                .callEvent(new FoundGameAddPlayer(player, role));
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
                    .callEvent(new FoundGameRemovePlayer(player, role.get()));
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

}
