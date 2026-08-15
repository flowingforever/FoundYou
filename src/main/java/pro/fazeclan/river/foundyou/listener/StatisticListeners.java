package pro.fazeclan.river.foundyou.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.GamePlayerDeathEvent;
import pro.fazeclan.river.foundyou.event.GamePlayerEliminationEvent;
import pro.fazeclan.river.foundyou.event.GamePlayerLoseEvent;
import pro.fazeclan.river.foundyou.event.GamePlayerWinEvent;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.jarona.Jarona;

public class StatisticListeners implements Listener {

    @EventHandler
    private void onDeath(GamePlayerDeathEvent event) {
        if (event.getRole().getFaction().equals(Faction.HUNTERS)) {
            Jarona.getInstance()
                    .getStatisticManager()
                    .incrementStatistic(
                            event.getPlayer().getUniqueId(),
                            FoundYou.getKey("game"),
                            FoundYou.getKey("deaths_as_hunter"),
                            1
                    );
        } else {
            Jarona.getInstance()
                    .getStatisticManager()
                    .incrementStatistic(
                            event.getPlayer().getUniqueId(),
                            FoundYou.getKey("game"),
                            FoundYou.getKey("deaths_as_runner"),
                            1
                    );
        }
    }

    @EventHandler
    private void onKill(GamePlayerEliminationEvent event) {
        if (event.getEliminatorRole().getFaction().equals(Faction.HUNTERS)) {
            Jarona.getInstance()
                    .getStatisticManager()
                    .incrementStatistic(
                            event.getEliminator().getUniqueId(),
                            FoundYou.getKey("game"),
                            FoundYou.getKey("kills_as_hunter"),
                            1
                    );
        } else {
            Jarona.getInstance()
                    .getStatisticManager()
                    .incrementStatistic(
                            event.getEliminator().getUniqueId(),
                            FoundYou.getKey("game"),
                            FoundYou.getKey("kills_as_runner"),
                            1
                    );
        }
    }

    @EventHandler
    private void onWin(GamePlayerWinEvent event) {
        if (event.getFaction().equals(Faction.HUNTERS)) {
            Jarona.getInstance().getStatisticManager().incrementStatistic(
                    event.getPlayer().getUniqueId(),
                    FoundYou.getKey("game"),
                    FoundYou.getKey("wins_as_hunter"),
                    1
            );
        } else {
            Jarona.getInstance().getStatisticManager().incrementStatistic(
                    event.getPlayer().getUniqueId(),
                    FoundYou.getKey("game"),
                    FoundYou.getKey("wins_as_runner"),
                    1
            );
        }
    }

}
