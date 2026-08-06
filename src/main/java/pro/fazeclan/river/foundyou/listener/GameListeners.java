package pro.fazeclan.river.foundyou.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.event.GamePlayerDeathEvent;
import pro.fazeclan.river.foundyou.util.RoleUtil;
import pro.fazeclan.river.foundyou.util.TimeUtil;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.GameUtil;

import java.io.File;
import java.util.UUID;

public class GameListeners implements Listener {

    @EventHandler
    public void handleEntityDamageEvent(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!player.getWorld().getKey().namespace().equals("foundyou")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerDropItem(PlayerDropItemEvent event) {
        if (!event.getPlayer().getWorld().getKey().namespace().equals("foundyou")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerInteractFarmland(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        if (!player.getWorld().getKey().namespace().equals("foundyou")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handleFoodLevel(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.getWorld().getKey().namespace().equals("foundyou")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerInteractChiseledBookshelf(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (!player.getWorld().getKey().namespace().equals("foundyou")) {
            return;
        }
        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != Material.CHISELED_BOOKSHELF) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerInteractPot(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (!player.getWorld().getKey().namespace().equals("foundyou")) {
            return;
        }
        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != Material.DECORATED_POT) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerInteractShelf(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (!player.getWorld().getKey().namespace().equals("foundyou")) {
            return;
        }
        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (!block.getType().getKey().value().contains("shelf")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerOnPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }
        if (!victim.getWorld().getKey().namespace().equals("foundyou")) {
            return;
        }
        if (!RoleUtil.isSameFaction(victim, damager)) {
            return;
        }
        if (FoundYou.getInstance().getConfig().getBoolean("friendly-fire")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void handlePlayerDamage(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.getWorld().getKey().namespace().equals("foundyou")) {
            return;
        }
        if (event.getFinalDamage() < player.getHealth()) {
            return;
        }
        if (player.getGameMode().isInvulnerable()) {
            player.teleport(player.getWorld().getSpawnLocation());
            return;
        }

        RoleUtil.eliminatePlayer(player);

        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        event.setDamage(0.1);
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (GameUtil.hasGame(player.getWorld())) return;

        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(player).clear();
        RoleUtil.removeRoles(player);
    }

}
