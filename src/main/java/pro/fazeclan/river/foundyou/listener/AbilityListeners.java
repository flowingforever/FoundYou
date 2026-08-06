package pro.fazeclan.river.foundyou.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.util.RoleUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class AbilityListeners implements Listener {

    @EventHandler
    public void handleSwapHands(PlayerSwapHandItemsEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.SPECTATOR)) return;
        var role = RoleUtil.getRole(player);
        if (role.isEmpty()) return;
        if (player.getScoreboardTags().contains("foundyoumuzzled")) return;
        for (String abilityId : role.get().getAbilities()) {
            FoundYou.getInstance().getServer().getPluginManager().callEvent(new AbilityEvent(
                    player,
                    abilityId
            ));
            if (!event.isCancelled()) {
                event.setCancelled(true);
            }
        }
    }

}
