package de.nvclas.flats.core.events;

import de.nvclas.flats.core.volumes.Flat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that is triggered when a {@link Flat} is created, updated, or deleted.
 * <p>
 * This event provides details about the affected {@link Flat} and the {@link Action} performed.
 * The available actions are defined in the {@link FlatUpdateEvent.Action} enum.
 * <p>
 * Listeners can use this event to react to changes in the state or existence of a flat.
 */
@Getter
@AllArgsConstructor
public class FlatUpdateEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Flat flat;
    private final Action action;

    @SuppressWarnings("unused") // Required for custom event
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @SuppressWarnings("java:S4144") // Required for custom event
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }
}
