package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.AbilityEvent;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GamePlayerDeathEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.foundyou.util.RoleUtil;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.SwitchCondition;
import pro.fazeclan.river.jarona.util.ConditionUtil;

import java.io.IOException;

public class ExplodeAbility extends Ability {

    public ExplodeAbility() {
        super("explode");
    }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;
        var player = event.getPlayer();

        var condition = ConditionUtil.getPlayerConditions(player)
                        .getOrCreate(
                                getId() + "_ability",
                                new SwitchCondition(
                                        false
                                )
                        );
        if (!condition.getAvailable()) return;

        handleExplosion(player);
        RoleUtil.eliminatePlayer(player);
        condition.setAvailable(false);
    }

    @EventHandler
    private void handlePlayerAddition(GameAddPlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        initializeAbilitySwitchCondition(event, manager, c -> {
            var sc = (SwitchCondition) c;
            if (sc.getAvailable()) {
                return "<red><sprite:blocks:block/tnt_side> Explode!</red>";
            } else {
                return "<red><sprite:blocks:block/tnt_side> <gray>Exploded.</gray></red>";
            }
        });
    }

    @EventHandler
    private void handlePlayerRemoval(GameRemovePlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(player).remove(getId() + "_ability");
    }

    @EventHandler
    public void onPlayerDeath(GamePlayerDeathEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        handleExplosion(event.getPlayer());
    }

    private void handleExplosion(Player player) {
        Location deathLocation = player.getLocation();

        createExplosionEffect(deathLocation);
        damageNearbyPlayers(player, deathLocation);
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

