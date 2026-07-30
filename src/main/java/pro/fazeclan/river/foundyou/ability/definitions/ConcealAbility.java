package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.foundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.condition.TimedUseCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.util.EnumMap;
import java.util.Map;

public class ConcealAbility extends Ability {

    public ConcealAbility() {
        super("conceal");
    }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var conditionManager = Jarona.getInstance().getConditionManager();
        var player = event.getPlayer();

        // uses cap
        int maxUses = getDefaultAbilityProperty("uses", 2);

        // cooldown
        var condition = conditionManager.getPlayerConditions(player).getOrCreate(
                getId() + "_ability",
                new TimedUseCondition(
                        TimedCondition.Type.GAME_TICK,
                        c -> null,
                        player.getUniqueId(),
                        maxUses
                )
        );

        if (!condition.getAvailable()) {
            return;
        }

        // update ui
        var cooldown = getDefaultAbilityProperty("cooldown", 30);

        condition.setDuration(cooldown * 20L);
        condition.increaseUses();

        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if (tc.getUses() >= tc.getMaxUses()) {
                return "<dark_gray>░ <gray>Depleted.</gray></dark_gray> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<dark_gray>░ <green>Ready!</green></dark_gray> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<dark_gray>░ <red>" + duration +"s</red></dark_gray> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });

        // Apply Invisibility for 8 seconds.

        var duration = getDefaultAbilityProperty("duration", 8);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration * 20, 0, false, false, true));
        hideArmor(player);

        SchedulingUtil.runLater(duration * 20L, () -> {
            showArmor(player);
        });

        player.sendMessage(ChatColor.GREEN + "Conceal activated! " + ChatColor.GRAY + "(Invisibility, " + duration + "s) "
                + ChatColor.DARK_AQUA + "[" + (condition.getMaxUses() - condition.getUses()) + " left]");
    }

    private void hideArmor(Player invisiblePlayer) {
        Map<EquipmentSlot, ItemStack> hiddenEquipment =
                new EnumMap<>(EquipmentSlot.class);

        hiddenEquipment.put(EquipmentSlot.HEAD, ItemStack.empty());
        hiddenEquipment.put(EquipmentSlot.CHEST, ItemStack.empty());
        hiddenEquipment.put(EquipmentSlot.LEGS, ItemStack.empty());
        hiddenEquipment.put(EquipmentSlot.FEET, ItemStack.empty());

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(invisiblePlayer)) {
                continue;
            }

            viewer.sendEquipmentChange(invisiblePlayer, hiddenEquipment);
        }
    }

    private void showArmor(Player player) {
        Map<EquipmentSlot, ItemStack> realEquipment =
                new EnumMap<>(EquipmentSlot.class);

        realEquipment.put(
                EquipmentSlot.HEAD,
                player.getInventory().getHelmet()
        );

        realEquipment.put(
                EquipmentSlot.CHEST,
                player.getInventory().getChestplate()
        );

        realEquipment.put(
                EquipmentSlot.LEGS,
                player.getInventory().getLeggings()
        );

        realEquipment.put(
                EquipmentSlot.FEET,
                player.getInventory().getBoots()
        );

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) {
                continue;
            }

            viewer.sendEquipmentChange(player, realEquipment);
        }
    }

    @EventHandler
    private void handleGamePlayerAdd(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var conditionManager = Jarona.getInstance().getConditionManager();
        int maxUses = getDefaultAbilityProperty("uses", 2);
        initializeAbilityUsesCondition(
                event,
                maxUses,
                conditionManager,
                c -> "<dark_gray>░ <green>Ready!</green></dark_gray> " + buildUses(maxUses, 0)
        );
    }

    @EventHandler
    private void handleGamePlayerRemoval(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var conditionManager = Jarona.getInstance().getConditionManager();
        conditionManager.getPlayerConditions(event.getPlayer())
                .remove(getId() + "_ability");
    }

}
