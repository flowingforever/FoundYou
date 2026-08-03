package pro.fazeclan.river.foundyou.ability.definitions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;

public class LaserbeamAbility extends Ability {

    private static final int SPEED_DURATION_TICKS = 4 * 20;
    private static final int GLOWING_DURATION_TICKS = 2 * 20;

    private static final Particle.DustOptions BLUE_DUST =
            new Particle.DustOptions(
                    Color.fromRGB(40, 40, 225), 1.1f
            );

    public LaserbeamAbility() {
        super("laser");
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

        int cooldown;

        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if ((tc.getDuration() / 20.0) == 0.0) {
                return "<blue>⧈ <green>Ready!</green></blue>";
            } else {
                return "<blue>⧈ <red>" + duration + "s</red></blue>";
            }
        });

        // ability

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1.0f, 2f);
        }

        if (fireBeam(player)) {
            cooldown = getDefaultAbilityProperty("success-cooldown", 3);
            world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.0f, 2f);
        } else {
            cooldown = getDefaultAbilityProperty("failed-cooldown", 20);
        }

        condition.setDuration(cooldown * 20L);

    }

    private boolean fireBeam(Player shooter) {

        // Start around the player's chest rather than their eyes.
        // The direction still follows where the player is looking.

        Location start = shooter.getLocation().add(0.0, 1.2, 0.0);

        Vector direction = shooter.getEyeLocation()
                .getDirection()
                .normalize();

        World world = shooter.getWorld();

        RayTraceResult result = world.rayTrace(
                start,
                direction,
                getDefaultAbilityProperty("beam-range", 7.0),
                FluidCollisionMode.NEVER,
                true,
                getDefaultAbilityProperty("hitbox-size", 0.35),
                entity -> isValidTarget(shooter, entity)
        );

        double beamLength = getDefaultAbilityProperty("beam-range", 7.0);
        Player hitPlayer = null;

        if (result != null) {
            beamLength = result.getHitPosition()
                    .distance(start.toVector());

            if (result.getHitEntity() instanceof Player target) {
                hitPlayer = target;
            }
        }

        spawnBeamParticles(world, start, direction, beamLength);

        assert hitPlayer != null;
        if (!hitPlayer.getGameMode().equals(GameMode.SPECTATOR)) {
            tellPlayerTheyMissed(shooter);
            return false;
        }

        handleSuccessfulHit(shooter, hitPlayer);
        return true;
    }

    private void spawnBeamParticles(World world, Location start, Vector direction, double beamLength) {
        for (double distance = 0.0; distance <= beamLength; distance += getDefaultAbilityProperty("beam-step", 7.0)) {
            Location particleLocation = start.clone().add(
                    direction.clone().multiply(distance)
            );

            world.spawnParticle(
                    Particle.DUST,
                    particleLocation,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    BLUE_DUST
            );
        }
    }


    private boolean isValidTarget(Player shooter, Entity entity) {
        return entity instanceof Player target
                && target != shooter
                && target.isValid()
                && !target.isDead();
    }

    private void handleSuccessfulHit(Player shooter, Player target) {
        target.damage(getDefaultAbilityProperty("damage", 6), shooter);

        shooter.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                SPEED_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        revealNearbyPlayers(target.getLocation());

        tellPlayerTheyHit(shooter, target);
    }

    private void revealNearbyPlayers(Location center) {
        double radiusSquared = getDefaultAbilityProperty("reveal-radius", 7) * getDefaultAbilityProperty("reveal-radius", 7);

        for (Player nearbyPlayer : center.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distanceSquared(center) > radiusSquared
                && !nearbyPlayer.getGameMode().equals(GameMode.SPECTATOR)) {
                continue;
            }

            nearbyPlayer.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING,
                    GLOWING_DURATION_TICKS,
                    0,
                    false,
                    false,
                    true
            ));
        }
    }

    private void tellPlayerTheyHit(Player shooter, Player target) {
        shooter.sendMessage(
                Component.text("TARGET DETECTED: ", NamedTextColor.GREEN)
                        .append(Component.text(
                                target.getName(),
                                NamedTextColor.YELLOW
                        ))
        );
    }

    private void tellPlayerTheyMissed(Player shooter) {
        shooter.sendMessage(
                Component.text(
                        "NO TARGET DETECTED.",
                        NamedTextColor.RED
                )
        );
    }


    @EventHandler
    private void handlePlayerAdd(GameAddPlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        initializeAbilityCondition(event, manager, c -> "<blue>⧈ <green>Ready!</green></blue>");
    }

    @EventHandler
    private void handlePlayerRemoval(GameRemovePlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(player).remove(getId() + "_ability");
    }

}
