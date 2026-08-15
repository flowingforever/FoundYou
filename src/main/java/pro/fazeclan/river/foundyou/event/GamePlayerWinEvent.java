package pro.fazeclan.river.foundyou.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import pro.fazeclan.river.foundyou.role.Faction;

public class GamePlayerWinEvent extends PlayerEvent {

    @Getter
    private final Faction faction;

    public GamePlayerWinEvent(@NotNull Player player, Faction faction) {
        super(player);
        this.faction = faction;
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
