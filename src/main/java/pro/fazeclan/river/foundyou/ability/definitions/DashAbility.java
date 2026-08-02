package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.condition.TimedUseCondition;

public class DashAbility extends Ability {
    public DashAbility() {
        super("dash");
    }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        var cooldown = getDefaultAbilityProperty("cooldown", 20) * 20L;
        var player = event.getPlayer();

        var maxUses = getDefaultAbilityProperty("uses", 3);

        var condition = manager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedUseCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null
                        )
                );

        if (!condition.getAvailable()) return;

        condition.setDuration(cooldown);

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 2f);
            world.spawnParticle(Particle.CLOUD, loc, 1, 0, 0, 0);
        }

        dash(player);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                2 * 20,
                1,
                false,
                false,
                true
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.DARKNESS,
                5 * 20,
                1,
                false,
                false,
                true
        ));

        player.sendMessage(ChatColor.GREEN + "Draining Dash activated! " + ChatColor.GRAY + "(Speed II, 2s. Darkness 5s.)");
        condition.increaseUses();

        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if (tc.getUses() >= tc.getMaxUses()) {
                return "<green>\uD83D\uDC5F <gray>Depleted.</gray></green> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<green>\uD83D\uDC5F <green>Ready!</green></green> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<green>\uD83D\uDC5F <red>" + duration +"s</red></green> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });

    }

    private void dash(Player player) {

        Vector dashVector = player.getEyeLocation()
                .getDirection()
                .setY(0)
                .normalize()
                .multiply(getDefaultAbilityProperty("magnitude", 1.11));

        player.setVelocity(dashVector);

    }

    @EventHandler
    private void handleGameAddPlayer(GameAddPlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) {
            return;
        }

        var conditionManager = Jarona.getInstance().getConditionManager();
        int maxUses = getDefaultAbilityProperty("uses", 2);
        initializeAbilityUsesCondition(
                event,
                maxUses,
                conditionManager,
                c -> "<green>\uD83D\uDC5F <green>Ready!</green></green> " + buildUses(maxUses, 0)
        );
    }

    @EventHandler
    private void handleGameRemovePlayer(GameRemovePlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var conditionManager = Jarona.getInstance().getConditionManager();
        conditionManager.getPlayerConditions(event.getPlayer())
                .remove(getId() + "_ability");
    }


}
