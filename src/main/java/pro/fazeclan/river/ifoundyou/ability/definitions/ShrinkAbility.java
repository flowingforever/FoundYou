package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.AbilityEvent;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.condition.TimedUseCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShrinkAbility extends Ability {

    private final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();  // primed window end
    private final Map<UUID, Closeable> TASK_MAP = new ConcurrentHashMap<>();

    private final Map<UUID, Double> originalScaleValues = new HashMap<>();

    private final double SMALL_SCALE = 0.65;

    public ShrinkAbility() { super("shrink"); }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;
        var conditionManager = Jarona.getInstance().getConditionManager();

        var cooldown = getDefaultAbilityProperty("cooldown", 45) * 20L;
        var player = event.getPlayer();

        var maxUses = getDefaultAbilityProperty("uses", 2);

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
                10 * 20,
                1,
                false,
                false,
                true
        ));

        AttributeInstance small = player.getAttribute(Attribute.SCALE);

        originalScaleValues.put(player.getUniqueId(), small.getBaseValue());
        ACTIVE_UNTIL.put(player.getUniqueId(), System.currentTimeMillis() + (getDefaultAbilityProperty("duration", 5) * 1000L));
        small.setBaseValue(SMALL_SCALE);

        condition.increaseUses();

        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if (ACTIVE_UNTIL.containsKey(c.getPlayerUUID())) {
                return "<dark_aqua>⬇ <yellow>Active!</yellow></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            }

            if (tc.getUses() >= tc.getMaxUses()) {
                return "<dark_aqua>⬇ <gray>Depleted.</gray></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<dark_aqua>⬇ <green>Ready!</green></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<dark_aqua>⬇ <red>" + duration +"s</red></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });

        player.sendMessage(ChatColor.GREEN + "Shrink activated! " + ChatColor.GRAY + "(Speed II & Shrink, 10s) "
                + ChatColor.DARK_AQUA + "[" + (condition.getMaxUses() - condition.getUses()) + " left]");

    }

    @EventHandler
    private void handleGameAddPlayer(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) {
            return;
        }

        var player = event.getPlayer();
        var conditionManager = Jarona.getInstance().getConditionManager();
        int maxUses = getDefaultAbilityProperty("uses", 2);
        initializeAbilityUsesCondition(
                event,
                maxUses,
                conditionManager,
                c -> "<dark_aqua>⬇ <green>Ready!</green></dark_aqua> " + buildUses(maxUses, 0)
        );

        AttributeInstance small = player.getAttribute(Attribute.SCALE);

        TASK_MAP.put(player.getUniqueId(), SchedulingUtil.interval(0L, 10L, () -> {
            Long activeUntil = ACTIVE_UNTIL.get(player.getUniqueId());
            if (activeUntil == null || activeUntil <= System.currentTimeMillis()) {
                small.setBaseValue(1);
            }
        }));
    }

    @EventHandler
    private void handleGameRemovePlayer(FoundGameRemovePlayer event) {
        var conditionManager = Jarona.getInstance().getConditionManager();
        conditionManager.getPlayerConditions(event.getPlayer())
                .remove(getId() + "_ability");

        try { TASK_MAP.remove(event.getPlayer().getUniqueId()).close(); } catch (IOException ignored) {}
    }

}
