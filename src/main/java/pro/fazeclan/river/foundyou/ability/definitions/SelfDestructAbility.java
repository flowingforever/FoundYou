package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.GamePlayerDeathEvent;

public class SelfDestructAbility extends Ability {

    public SelfDestructAbility() {
        super("explode");
    }

    @EventHandler
    public void onPlayerDeath(GamePlayerDeathEvent event) {

        if (!event.getRole().getAbilities().contains(getId())) {
            return;
        }

        Player deadPlayer = event.getPlayer();
        Location deathLocation = deadPlayer.getLocation();

        createExplosionEffect(deathLocation);
        damageNearbyPlayers(deadPlayer, deathLocation);
    }

    private void createExplosionEffect(Location location) {
        location.getWorld().spawnParticle(
                Particle.EXPLOSION,
                location,
                1
        );

        location.getWorld().playSound(
                location,
                Sound.ENTITY_GENERIC_EXPLODE,
                1.0F,
                0.7F
        );
    }

    private void damageNearbyPlayers(Player deadPlayer, Location location) {
        var radius = getDefaultAbilityProperty("radius", 5);
        var damage = getDefaultAbilityProperty("damage", 10);
        for (Player nearbyPlayer : location.getNearbyPlayers(radius)) {
            if (nearbyPlayer.equals(deadPlayer)) {
                continue;
            }

            if (nearbyPlayer.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            nearbyPlayer.damage(damage, deadPlayer);
        }
    }
}

