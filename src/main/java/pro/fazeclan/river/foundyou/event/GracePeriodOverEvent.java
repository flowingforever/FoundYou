package pro.fazeclan.river.foundyou.event;

import lombok.Getter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GracePeriodOverEvent extends Event {
    @Getter
    private final List<Player> players;

    @Getter
    private final World world;

    public GracePeriodOverEvent(World world, List<Player> players) {
        this.world = world;
        this.players = players;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private static HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
