package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.util.Vector;
import pro.fazeclan.river.foundyou.ability.Ability;

public class QuickfireAbility extends Ability {

    public QuickfireAbility() {
        super("quickfire", ItemType.ARROW.createItemStack());
    }

    @EventHandler
    public void onPlayerLeftClick(PlayerInteractEvent event) {
        // Prevent the event from running for the offhand.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();

        if (action != Action.LEFT_CLICK_AIR
                && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (mainHand.getType() != Material.CROSSBOW) {
            return;
        }

        // Prevent shooting while the crossbow is on cooldown.
        if (player.hasCooldown(Material.CROSSBOW)) {
            return;
        }

        // The ability requires an arrow.
        if (!player.getInventory().contains(Material.ARROW)) {
            return;
        }

        event.setCancelled(true);

        consumeArrow(player);
        shootArrow(player);

        // Display Minecraft cooldown overlay over the crossbow.
        player.setCooldown(Material.CROSSBOW, getDefaultAbilityProperty("cooldown-ticks", 30));
    }

    private void consumeArrow(Player player) {
        player.getInventory().removeItem(
                new ItemStack(Material.ARROW, 1)
        );
    }

    private void shootArrow(Player player) {
        Vector velocity = player.getEyeLocation()
                .getDirection()
                .normalize()
                .multiply(getDefaultAbilityProperty("arrow-speed", 3.0));

        Arrow arrow = player.launchProjectile(Arrow.class, velocity);

        // TODO: Remove line if this becomes annoying.
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
    }
}