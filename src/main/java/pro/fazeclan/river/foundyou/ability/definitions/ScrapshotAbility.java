package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;

public class ScrapshotAbility extends Ability {

    public ScrapshotAbility() {
        super("scrapshot", ItemType.CROSSBOW.createItemStack());
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

        consumeArrow(player);
        fireParticleBeam(player);
        applyRecoil(player);

        player.setCooldown(Material.CROSSBOW, getDefaultAbilityProperty("cooldown", 20) * 20);

        player.sendMessage(ChatColor.GOLD + "Recoil shot fired! " + ChatColor.GRAY + "(" + cooldown + "s cooldown.)");
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
                getDefaultAbilityProperty("beam-range", 5.0),
                FluidCollisionMode.NEVER,
                true,
                getDefaultAbilityProperty("hitbox-size", 0.35),
                entity -> isValidTarget(player, entity)
        );

        double beamLength = getDefaultAbilityProperty("beam-range", 5.0);

        if (result != null) {
            beamLength = result.getHitPosition()
                    .distance(start.toVector());

            Entity hitEntity = result.getHitEntity();

            if (hitEntity instanceof LivingEntity target) {
                target.damage(getDefaultAbilityProperty("damage", 12), player);
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
        for (double distance = 0; distance <= length; distance += getDefaultAbilityProperty("beam-step", 0.25)) {
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
                    new Particle.DustOptions(
                            Color.fromRGB(
                                    getDefaultAbilityProperty("particle.red", 255),
                                    getDefaultAbilityProperty("particle.green", 125),
                                    getDefaultAbilityProperty("particle.blue", 20)
                            ),
                            getDefaultAbilityProperty("particle.size", 1.25).floatValue()
                    )
            );
        }
    }

    private void applyRecoil(Player player) {
        Vector recoil = player.getEyeLocation()
                .getDirection()
                .normalize()
                .multiply(-getDefaultAbilityProperty("recoil-strength", 1.29));

        recoil.setY(Math.max(recoil.getY(), getDefaultAbilityProperty("recoil-verticality", 0.34)));

        player.setVelocity(recoil);
    }


    @EventHandler
    private void handlePlayerAdd(GameAddPlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        initializeAbilityCondition(event, manager, c -> "<gold>\uD83C\uDFF9 <green>Ready!</green></gold>");
    }

    @EventHandler
    private void handlePlayerRemoval(GameRemovePlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(player).remove(getId() + "_ability");
    }

}
