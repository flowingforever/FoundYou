package pro.fazeclan.river.foundyou.listener;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.util.RoleUtil;

public class AbilityListeners implements Listener {

    @EventHandler
    public void handleSwapHands(PlayerSwapHandItemsEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.SPECTATOR)) {
            return;
        }
        var role = RoleUtil.getRole(player);
        if (role.isEmpty()) {
            return;
        }
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
