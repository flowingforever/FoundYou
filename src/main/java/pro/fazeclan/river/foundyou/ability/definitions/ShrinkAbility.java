package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.condition.TimedUseCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

public class ShrinkAbility extends Ability {

    public ShrinkAbility() { super("shrink", ItemType.ANVIL.createItemStack()); }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;
        var conditionManager = Jarona.getInstance().getConditionManager();

        var cooldown = getDefaultAbilityProperty("cooldown", 45) * 20L;
        var player = event.getPlayer();
        var previousLocation = player.getLocation();

        var maxUses = getDefaultAbilityProperty("uses", 2);

        var abilityDuration = getDefaultAbilityProperty("duration", 5);

        var condition = conditionManager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedUseCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null,
                                player.getUniqueId(),
                                maxUses
                        )
                );

        if (!condition.getAvailable()) return;

        condition.setDuration(cooldown);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                abilityDuration * 20,
                1,
                false,
                false,
                true
        ));

        AttributeInstance small = player.getAttribute(Attribute.SCALE);

        SchedulingUtil.runLater(abilityDuration * 20L, () -> {
            small.setBaseValue(1);

            if (player.getLocation().getBlock().getBoundingBox().contains(player.getBoundingBox())) {
                player.teleport(previousLocation);
            }
        });
        small.setBaseValue(getDefaultAbilityProperty("small-scale", 0.65));

        condition.increaseUses();

        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if (tc.getUses() >= tc.getMaxUses()) {
                return "<dark_aqua>⬇ <gray>Depleted.</gray></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<dark_aqua>⬇ <green>Ready!</green></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<dark_aqua>⬇ <red>" + duration +"s</red></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });

        player.sendMessage(ChatColor.GREEN + "Shrink activated! " + ChatColor.GRAY + "(Speed II & Shrink, " + abilityDuration + "s) "
                + ChatColor.DARK_AQUA + "[" + (condition.getMaxUses() - condition.getUses()) + " left]");

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
                c -> "<dark_aqua>⬇ <green>Ready!</green></dark_aqua> " + buildUses(maxUses, 0)
        );

    }

    @EventHandler
    private void handleGameRemovePlayer(GameRemovePlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        var conditionManager = Jarona.getInstance().getConditionManager();
        conditionManager.getPlayerConditions(player)
                .remove(getId() + "_ability");

        player.getAttribute(Attribute.SCALE).setBaseValue(1);
    }

}
