package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.foundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;

import org.bukkit.util.Vector;

public class ScrapshotAbility extends Ability {

    private static final double BEAM_RANGE = 5.0;
    private static final double BEAM_STEP = 0.25;
    private static final double HITBOX_SIZE = 0.35;

    private static final Particle.DustOptions ORANGE_DUST = new Particle.DustOptions(Color.fromRGB(255, 125, 20), 1.25f);

    public ScrapshotAbility() {
        super("scrapshot");
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

        int cooldown = getDefaultAbilityProperty("cooldown", 30);

        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if ((tc.getDuration() / 20.0) == 0.0) {
                return "<gold>\uD83C\uDFF9 <green>Ready!</green></gold>";
            } else {
                return "<gold>\uD83C\uDFF9 <red>" + duration + "s</red></gold>";
            }
        });

        condition.setDuration(cooldown * 20L);

        // ability
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world != null) {

            world.playSound(loc, Sound.ENTITY_ARROW_HIT, 1.0f, 0.5f);
            world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.5f);
        }

        ItemStack crossbow = player.getInventory().getItemInMainHand();

        if (crossbow.getType() != Material.CROSSBOW) {
            return;
        }

        if (player.hasCooldown(Material.CROSSBOW)) {
            return;
        }

        if (!player.getInventory().contains(Material.ARROW)) {
            return;
        }

        consumeArrow(player);
        fireParticleBeam(player);
        applyRecoil(player);

        player.setCooldown(Material.CROSSBOW, getDefaultAbilityProperty("cooldown", 30));
    }

    private void consumeArrow(Player player) {
        player.getInventory().removeItem(
                new ItemStack(Material.ARROW, 1)
        );
    }

    private void fireParticleBeam(Player player) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        World world = player.getWorld();

        RayTraceResult result = world.rayTrace(
                start,
                direction,
                BEAM_RANGE,
                FluidCollisionMode.NEVER,
                true,
                HITBOX_SIZE,
                entity -> isValidTarget(player, entity)
        );

        double beamLength = BEAM_RANGE;

        if (result != null) {
            beamLength = result.getHitPosition()
                    .distance(start.toVector());

            Entity hitEntity = result.getHitEntity();

            if (hitEntity instanceof LivingEntity target) {
                target.damage(getDefaultAbilityProperty("damage", 8), player);
            }
        }

        spawnBeamParticles(world, start, direction, beamLength);
    }

    private boolean isValidTarget(Player shooter, Entity entity) {
        return entity instanceof LivingEntity
                && entity != shooter
                && entity.isValid()
                && !entity.isDead();
    }

    private void spawnBeamParticles(World world, Location start, Vector direction, double length) {
        for (double distance = 0; distance <= length; distance += BEAM_STEP) {
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
                    ORANGE_DUST
            );
        }
    }

    private void applyRecoil(Player player) {
        Vector recoil = player.getEyeLocation()
                .getDirection()
                .normalize()
                .multiply(-getDefaultAbilityProperty("recoil-strength", 1.4));

        recoil.setY(Math.max(recoil.getY(), getDefaultAbilityProperty("recoil-verticality", 0.35)));

        player.setVelocity(recoil);
    }


    @EventHandler
    private void handlePlayerAdd(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        initializeAbilityCondition(event, manager, c -> "<gold>\uD83C\uDFF9 <green>Ready!</green></gold>");
    }

    @EventHandler
    private void handlePlayerRemoval(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(player).remove(getId() + "_ability");
    }

}
