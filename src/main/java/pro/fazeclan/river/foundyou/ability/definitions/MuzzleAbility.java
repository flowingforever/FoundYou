package pro.fazeclan.river.foundyou.ability.definitions;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.ConditionUtil;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

public class MuzzleAbility extends Ability {

    public MuzzleAbility() { super("muzzle"); }

    @EventHandler
    private void handleAbility(AbilityEvent event) {

        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;
        var player = event.getPlayer();

        var condition = ConditionUtil.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null,
                                player.getUniqueId()
                        )
                );

        if (!condition.getAvailable()) return;
        condition.setDuration(getDefaultAbilityProperty("cooldown", 45) * 20L);

        var range = getDefaultAbilityProperty("range", 7.0);
        var duration = getDefaultAbilityProperty("duration", 10) * 20;
        final BossBar bossbar = BossBar.bossBar(
                Component.text("Muzzled!").color(NamedTextColor.GRAY),
                1f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS
        );
        bossbar.addListener(new MuzzleListener());
        new BukkitRunnable() {
            long tick = 0;

            @Override
            public void run() {
                tick++;
                bossbar.progress(Math.min(1.0f - (float) tick / duration, 1.0f));

                if (tick >= duration) {
                    cancel();
                }
            }

        }.runTaskTimer(FoundYou.getInstance(), 0L, 1L);
        for (var victim : player.getLocation().getNearbyPlayers(range)) {
            if (victim.equals(player)) continue;
            if (victim.getGameMode().isInvulnerable()) continue;

            bossbar.addViewer(victim);
            victim.addScoreboardTag("foundyoumuzzled");
        }

    }

    @EventHandler
    private void handlePlayerAddition(GameAddPlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        initializeAbilityCondition(event, manager, c -> {
            var sc = (TimedCondition) c;
            if (sc.getAvailable()) {
                return "<gray><sprite:items:item/goat_horn> Ready!</gray>";
            } else {
                return "<gray><sprite:items:item/goat_horn> " + (sc.getDuration() / 20) + "s</gray>";
            }
        });
    }

    @EventHandler
    private void handlePlayerRemoval(GameRemovePlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(player).remove(getId() + "_ability");
    }

    private class MuzzleListener implements BossBar.Listener {

        @Override
        public void bossBarProgressChanged(@NotNull BossBar bossbar, float oldProgress, float newProgress) {
            BossBar.Listener.super.bossBarProgressChanged(bossbar, oldProgress, newProgress);

            if (newProgress <= 0f) {
                var viewers = bossbar.viewers();
                viewers.forEach(v -> {
                    if (!(v instanceof Player viewer)) return;
                    bossbar.removeViewer(viewer);
                    viewer.removeScoreboardTag("foundyoumuzzled");
                });
            }
        }
    }

}
