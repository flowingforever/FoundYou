package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.SwitchCondition;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.ConditionUtil;

public class MuzzleAbility extends Ability {

    public MuzzleAbility() { super("muzzle", ItemType.WHITE_WOOL.createItemStack()); }

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

        for (var victim : player.getLocation().getNearbyPlayers(range)) {
            if (victim.equals(player)) continue;
            if (victim.getGameMode().isInvulnerable()) continue;

            victim.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, duration, 0, true, false, true));
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
        var player = event.getPlayer();
        player.removeScoreboardTag("foundyoumuzzled");
        ConditionUtil.getPlayerConditions(player).remove("foundyoumuzzled");

        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(player).remove(getId() + "_ability");
    }

}
