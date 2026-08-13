package pro.fazeclan.river.foundyou.util;

import de.tr7zw.nbtapi.NBT;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.jarona.util.GameUtil;

import java.util.Objects;

public class RoleUtil {

    public static void assignRole(Player player, Role role) {
        var worldUUID = player.getWorld().getUID();
        var game = GameUtil.getGame(player.getWorld());
        if (game == null) {
            return;
        }
        game.getGameValues(worldUUID).setValue(player.getUniqueId() + "_role", role);
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
    }

    public static void assignRole(Player player, Role role, Location teleport) {
        assignRole(player, role);
        player.teleport(teleport);
    }

    public static void removeRoles(Player player) {
        var worldUUID = player.getWorld().getUID();
        var game = GameUtil.getGame(player.getWorld());
        if (game == null) {
            return;
        }
        var gameValues = game.getGameValues(worldUUID);
        Role role = gameValues.getValue(player.getUniqueId() + "_role");
        if (role != null) {
            gameValues.removeValue(player.getUniqueId() + "_role");
            FoundYou.getInstance()
                    .getServer()
                    .getPluginManager()
                    .callEvent(new GameRemovePlayerEvent(player, role));
        }
    }

    public static boolean isRole(Player player, Role role) {
        return role.equals(getRole(player));
    }

    public static Role getRole(Player player) {
        var worldUUID = player.getWorld().getUID();
        var game = GameUtil.getGame(player.getWorld());
        if (game == null) {
            return null;
        }
        return game.getGameValues(worldUUID).getValue(player.getUniqueId() + "_role");
    }

    public static Faction getFaction(Player player) {
        var role = getRole(player);
        if (role == null) return null;
        return role.getFaction();
    }

    public static boolean isSameFaction(Player p1, Player p2) {
        return Objects.equals(getFaction(p1), getFaction(p2));
    }

}
