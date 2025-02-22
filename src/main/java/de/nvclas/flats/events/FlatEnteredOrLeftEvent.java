package de.nvclas.flats.events;

import de.nvclas.flats.volumes.Flat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is triggered when a {@link Player} enters or leaves a specific {@link Flat}.
 * <p>
 * The event provides information about the {@link Flat} involved, the {@link Player}
 * involved, and whether the event signifies entry or exit.
 * <p>
 * To determine whether the event represents an entry or exit, use {@link #hasEntered()}.
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

    /**
     * Determines whether the associated event signifies that a player has entered a {@link Flat}.
     * <p>
     * This method can be used to differentiate between entry and exit events in a
     * {@link FlatEnteredOrLeftEvent}.
     *
     * @return {@code true} if the player has entered the flat; {@code false} if the player has left.
     */
    public boolean hasEntered() {
        return entered;
    }

    @SuppressWarnings("java:S4144") // Required for custom event
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
