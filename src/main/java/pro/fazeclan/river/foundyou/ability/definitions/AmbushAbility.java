package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

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
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

public class AmbushAbility extends Ability {

    private final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();  // primed window end

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
                return "<light_gray>\uD83D\uDC80 <green>Ready!</green></light_gray>";
            } else {
                return "<light_gray>\uD83D\uDC80 <red>" + duration + "s</red></light_gray>";
            }
        });

        // ability
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.playSound(loc, Sound.ENTITY_ARROW_HIT, 1.0f, 0.5f);
            world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.5f);
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
        player.sendMessage(ChatColor.RED + "Ambush activated! " + ChatColor.GRAY + "(Invisible for 10s.)");

        SchedulingUtil.runLater(duration * 20L, () -> {
            ACTIVE_UNTIL.remove(player.getUniqueId());
            reveal(player);
        });

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

        if (isBehindPlayer(attacker, victim)) {
            event.setDamage(event.getDamage() * getDefaultAbilityProperty("backstab-multiplier", 1.5));
            reveal(attacker);
        } else {
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

    private void reveal(Player player) {
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private boolean isAbilityActive(Player player) {
        return ACTIVE_UNTIL.containsKey(player.getUniqueId());
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
                c -> "<light_gray>\uD83D\uDC80 <green>Ready!</green></light_gray> " + buildUses(maxUses, 0)
        );
    }

    @EventHandler
    private void handleGamePlayerRemove(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(event.getPlayer()).remove(getId() + "_ability");
    }
}
