package pro.fazeclan.river.foundyou.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import pro.fazeclan.river.foundyou.role.Faction;

import java.util.List;

public class GamePlayerWinEvent extends Event {

    @Getter
    private final List<Player> players;
    @Getter
    private final Faction faction;

    public GamePlayerWinEvent(Faction faction, List<Player> players) {
        this.players = players;
        this.faction = faction;
    }

    public GamePlayerWinEvent(Faction faction, Player... players) {
        this.players = List.of(players);
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
