package de.nvclas.flats.events;

import de.nvclas.flats.volumes.Flat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player enters or leaves a flat
 */
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

    @SuppressWarnings("unused") // Required for custom event
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public boolean hasEntered() {
        return entered;
    }

    @SuppressWarnings("java:S4144") // Required for custom event
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
