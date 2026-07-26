package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.AbilityEvent;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.ifoundyou.event.GamePlayerDeathEvent;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.condition.TimedUseCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class SelfDestruct extends Ability {

    private double EXPLOSION_RADIUS = 5;
    private double EXPLOSION_DAMAGE = 10;

    public SelfDestruct() {
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
        for (Player nearbyPlayer : location.getNearbyPlayers(EXPLOSION_RADIUS)) {
            if (nearbyPlayer.equals(deadPlayer)) {
                continue;
            }

            if (nearbyPlayer.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            nearbyPlayer.damage(EXPLOSION_DAMAGE, deadPlayer);
        }
    }
}

