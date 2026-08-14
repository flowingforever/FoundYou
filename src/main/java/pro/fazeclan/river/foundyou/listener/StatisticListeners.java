package pro.fazeclan.river.foundyou.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.GamePlayerDeathEvent;
import pro.fazeclan.river.jarona.Jarona;

public class StatisticListeners implements Listener {

    @EventHandler
    private void handlePlayerDeath(GamePlayerDeathEvent event) {
        Jarona.getInstance().getStatisticManager().incrementStatistic(
                event.getPlayer().getUniqueId(), FoundYou.getKey("game"), FoundYou.getKey("deaths_as_runner"), 1
        );
    }

}
