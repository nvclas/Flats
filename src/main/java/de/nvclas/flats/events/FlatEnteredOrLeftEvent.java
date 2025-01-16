package de.nvclas.flats.events;

import de.nvclas.flats.selection.Flat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class FlatEnteredOrLeftEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    @NotNull
    @Getter
    private final Flat flat;
    @NotNull
    @Getter
    private final Player player;
    private final boolean entered;

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public boolean hasEntered() {
        return entered;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
