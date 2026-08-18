package pro.fazeclan.river.foundyou.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.GamePlayerWinEvent;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.jarona.Jarona;

public class AchievementListeners implements Listener {

    @EventHandler
    private void onWin(GamePlayerWinEvent event) {
        if (event.getFaction().equals(Faction.RUNNERS)) {
            Jarona.getInstance()
                    .getAchievementManager()
                    .giveAchievement(
                            event.getPlayer().getUniqueId(),
                            FoundYou.getKey("game"),
                            FoundYou.getKey("runner_win")
                    );
        } else {
            Jarona.getInstance()
                    .getAchievementManager()
                    .giveAchievement(
                            event.getPlayer().getUniqueId(),
                            FoundYou.getKey("game"),
                            FoundYou.getKey("hunter_win")
                    );
        }
    }

}
