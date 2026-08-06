package pro.fazeclan.river.foundyou.ability.definitions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.condition.TimedUseCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RewindAbility extends Ability {

    private final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();

    // Particles
    private final Map<UUID, Closeable> PARTICLE_TASKS = new ConcurrentHashMap<>();

    public RewindAbility() {
        super("rewind");
    }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;
        var conditionManager = Jarona.getInstance().getConditionManager();

        var cooldown = getDefaultAbilityProperty("cooldown", 60) * 20L;
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

        int duration = getDefaultAbilityProperty("duration", 5);

        long now = System.currentTimeMillis();
        Long until = ACTIVE_UNTIL.get(player.getUniqueId());
        if (until != null && until > now) {
            player.sendMessage(NamedTextColor.YELLOW + "Rewind is already active.");
            return;
        }

        condition.setDuration(cooldown);
        condition.increaseUses();

        ACTIVE_UNTIL.put(player.getUniqueId(), now + (duration * 1000L));

        // Save the player's exact position and facing direction.
        Location teleportLocation = player.getLocation();

        // Put the particles close to the ground rather than at the player's feet.
        Location particleLocation = teleportLocation.clone().add(0.0, 0.05, 0.0);

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_ENDER_EYE_LAUNCH,
                0.8f,
                1.0f
        );

        Closeable beamTask = SchedulingUtil.interval(0L, duration, () -> {
            if (!teleportLocation.isWorldLoaded()) return;
            World w = teleportLocation.getWorld();
            if (w == null) return;

            final double height = 1.25;
            final double step = 0.25;

            for (double dy = 0; dy <= height; dy += step) {
                w.spawnParticle(Particle.OMINOUS_SPAWNING, particleLocation, 4, 0.02, 0.02, 0.02, 0);
            }

        });
        PARTICLE_TASKS.put(player.getUniqueId(), beamTask);

        SchedulingUtil.runLater(duration * 20L, () -> {
            ACTIVE_UNTIL.remove(player.getUniqueId());
            teleportToMark(player, teleportLocation);

            Closeable task = PARTICLE_TASKS.remove(player.getUniqueId());
            if (task != null) {
                try {
                    task.close();
                } catch (IOException ignored) {}
            }

        });

        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            if (tc.getUses() >= tc.getMaxUses()) {
                return "<dark_aqua>⏰ <gray>Depleted.</gray></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<dark_aqua>⏰ <green>Ready!</green></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<dark_aqua>⏰ <red>" + duration + "s</red></dark_aqua> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });

        player.sendMessage(NamedTextColor.DARK_AQUA + "Rewind activated! " + NamedTextColor.GRAY + "(You will be teleported back in 5s.) "
                + NamedTextColor.RED + "[" + (condition.getMaxUses() - condition.getUses()) + " left]");

    }

    private void teleportToMark(Player player, Location teleportLocation) {
        boolean teleported = player.teleport(teleportLocation);

        if (!teleported) {
            player.sendMessage(NamedTextColor.RED + "Rewind failed!");
            return;
        }

        player.playSound(
                teleportLocation,
                Sound.ENTITY_ENDER_EYE_DEATH,
                1.0f,
                0.8f
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ACTIVE_UNTIL.remove(event.getPlayer().getUniqueId());

        Closeable task = PARTICLE_TASKS.remove(event.getPlayer().getUniqueId());
        if (task != null) {
            try {
                task.close();
            } catch (IOException ignored) {}
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        ACTIVE_UNTIL.remove(event.getPlayer().getUniqueId());

        Closeable task = PARTICLE_TASKS.remove(event.getPlayer().getUniqueId());
        if (task != null) {
            try {
                task.close();
            } catch (IOException ignored) {}
        }
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
                c -> "<dark_aqua>⏰ <green>Ready!</green></dark_aqua> " + buildUses(maxUses, 0)
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
