package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.foundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuiverAbility extends Ability {

    private final Map<UUID, Closeable> quiverTask = new HashMap<>();

    public QuiverAbility() {
        super("quiver");
    }

    @EventHandler
    private void handleGameAddPlayer(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) {
            return;
        }
        var player = event.getPlayer();
        int interval = getDefaultAbilityProperty("interval", 30);
        quiverTask.put(player.getUniqueId(), SchedulingUtil.interval(20, interval * 20L, () -> {
            giveArrow(player);
        }));
    }

    private void giveArrow(Player p) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        PlayerInventory inv = p.getInventory();
        var leftover = inv.addItem(arrow);

        if (!leftover.isEmpty()) {
            World w = p.getWorld();
            if (w != null) w.dropItemNaturally(p.getLocation(), arrow);
        }

        p.sendMessage(ChatColor.GREEN + "You've gained an arrow.");
    }

    @EventHandler
    private void handleGameRemovePlayer(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;
        try {
            quiverTask.remove(event.getPlayer().getUniqueId()).close();
        } catch (IOException ignored) {}
    }
}
