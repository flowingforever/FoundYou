package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.foundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.foundyou.event.GracePeriodOverEvent;
import pro.fazeclan.river.foundyou.util.RoleUtil;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

public class AmbushAbility extends Ability {

    private final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();  // primed window end

    private final Map<UUID, Integer> hiddenArrowCounts = new HashMap<>();

    public AmbushAbility() {
        super("ambush");
    }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        var player = event.getPlayer();

        var condition = manager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null
                        )
                );

        if (!condition.getAvailable()) return;

        int cooldown = getDefaultAbilityProperty("cooldown", 90);

        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if ((tc.getDuration() / 20.0) == 0.0) {
                return "<gray>☠ <green>Ready!</green></gray>";
            } else {
                return "<gray>☠ <red>" + duration + "s</red></gray>";
            }
        });

        // ability
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
        }

        int duration = getDefaultAbilityProperty("duration", 10);

        long now = System.currentTimeMillis();
        Long until = ACTIVE_UNTIL.get(player.getUniqueId());
        if (until != null && until > now) {
            player.sendMessage(ChatColor.YELLOW + "Ambush is already active.");
            return;
        }

        condition.setDuration(cooldown * 20L);

        ACTIVE_UNTIL.put(player.getUniqueId(), now + (duration * 1000L));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                duration * 20,
                0,
                false,
                false,
                true
        ));
        hideArmor(player);
        hideBodyArrows(player);
        player.removePotionEffect(PotionEffectType.GLOWING);

        player.sendMessage(ChatColor.RED + "Ambush activated! " + ChatColor.GRAY + "(Invisible for 10s.)");

        SchedulingUtil.runLater(duration * 20L, () -> {
            ACTIVE_UNTIL.remove(player.getUniqueId());
            reveal(player);
        });

    }

    private void reveal(Player player) {
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        showArmor(player);
        restoreBodyArrows(player);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING,
                PotionEffect.INFINITE_DURATION,
                0,
                false,
                false,
                true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInvisiblePlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!isAbilityActive(attacker)) {
            return;
        }

        Location loc = attacker.getLocation();
        World world = loc.getWorld();


        if (isBehindPlayer(attacker, victim)) {
            if (world != null) {
                world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.8f);
            }

            event.setDamage(event.getDamage() * getDefaultAbilityProperty("backstab-multiplier", 1.5));
            reveal(attacker);
        } else {
            if (world != null) {
                world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1f);
            }

            event.setDamage(event.getDamage() * getDefaultAbilityProperty("normal-hit-multiplier", 0.75));
            reveal(attacker);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInvisiblePlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!isAbilityActive(victim)) {
            return;
        }

        reveal(victim);

        Location loc = victim.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.1f);
        }

        victim.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS,
                (getDefaultAbilityProperty("punish-duration", 5) * 20),
                0,
                false,
                false,
                true
        ));
    }

    private boolean isBehindPlayer(Player attacker, Player victim) {
        // Horizontal direction in which the victim is facing.
        Vector victimFacing = victim.getLocation()
                .getDirection()
                .setY(0);


        // Horizontal direction from the victim toward the attacker.
        Vector victimToAttacker = attacker.getLocation()
                .toVector()
                .subtract(victim.getLocation().toVector())
                .setY(0);

        if (victimFacing.lengthSquared() == 0.0
                || victimToAttacker.lengthSquared() == 0.0) {
            return false;
        }

        victimFacing.normalize();
        victimToAttacker.normalize();

        double behindDot = victimFacing
                .multiply(-1)
                .dot(victimToAttacker);

        return behindDot >= getDefaultAbilityProperty("backstab-dot-threshold", 0.5);
    }

    private boolean isAbilityActive(Player player) {
        return ACTIVE_UNTIL.containsKey(player.getUniqueId());
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

    private void hideBodyArrows(Player player) {
        UUID playerId = player.getUniqueId();

        hiddenArrowCounts.put(
                playerId,
                player.getArrowsInBody()
        );

        player.setArrowsInBody(0, true);
    }

    private void restoreBodyArrows(Player player) {
        Integer arrowCount = hiddenArrowCounts.remove(
                player.getUniqueId()
        );

        if (arrowCount == null) {
            return;
        }

        player.setArrowsInBody(arrowCount, true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @EventHandler
    private void handleGamePlayerAdd(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        int maxUses = getDefaultAbilityProperty("uses", 2);
        initializeAbilityUsesCondition(
                event,
                maxUses,
                manager,
                c -> "<gray>☠ <green>Ready!</green></gray> " + buildUses(maxUses, 0)
        );
    }

    @EventHandler
    private void handleGamePlayerRemove(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(event.getPlayer()).remove(getId() + "_ability");
    }

    @EventHandler
    private void handleGraceOver(GracePeriodOverEvent event) {
        for (var player : event.getPlayers()) {
            if (RoleUtil.getRoleOrThrow(player).getAbilities().contains(getId())) {
                player.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
    }
}
