package pro.fazeclan.river.foundyou.ability.definitions;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import pro.fazeclan.river.foundyou.ability.Ability;
import pro.fazeclan.river.foundyou.event.GameAddPlayerEvent;
import pro.fazeclan.river.foundyou.event.GameRemovePlayerEvent;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuiverAbility extends Ability {

    private final Map<UUID, Closeable> quiverTask = new HashMap<>();

    public QuiverAbility() {
        super("quiver", ItemType.SPECTRAL_ARROW.createItemStack());
    }

    @EventHandler
    private void handleGameAddPlayer(GameAddPlayerEvent event) {
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
        p.give(arrow);

        p.sendMessage(ChatColor.GREEN + "You've gained an arrow.");
    }

    @EventHandler
    private void handleGameRemovePlayer(GameRemovePlayerEvent event) {
        if (!event.getRole().getAbilities().contains(getId())) return;
        try {
            quiverTask.remove(event.getPlayer().getUniqueId()).close();
        } catch (IOException ignored) {}
    }
}
