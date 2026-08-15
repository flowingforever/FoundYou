package pro.fazeclan.river.foundyou.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import pro.fazeclan.river.foundyou.role.Role;

public class GamePlayerEliminationEvent extends GamePlayerDeathEvent {

    @Getter
    private final Player eliminator;
    @Getter
    private final Role eliminatorRole;

    public GamePlayerEliminationEvent(@NotNull Player player, Role role, Player eliminator, Role eliminatorRole) {
        super(player, role);
        this.eliminator = eliminator;
        this.eliminatorRole = eliminatorRole;
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
