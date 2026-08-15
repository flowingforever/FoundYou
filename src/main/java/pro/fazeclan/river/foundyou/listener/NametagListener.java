package pro.fazeclan.river.foundyou.listener;

import org.alexdev.unlimitednametags.api.event.PlayerNametagVisibilityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.util.RoleUtil;
import pro.fazeclan.river.jarona.util.GameUtil;

public class NametagListener implements Listener {

    @EventHandler
    private void handleNametagVisibility(PlayerNametagVisibilityEvent event) {
        var viewer = event.getViewer();
        var target = event.getOwner();
        if (!GameUtil.hasGame(viewer.getWorld())) return;
        if (RoleUtil.isSameFaction(viewer, target) || RoleUtil.getFaction(viewer) == Faction.RUNNERS) return;
        event.setVisible(false);
    }

}
