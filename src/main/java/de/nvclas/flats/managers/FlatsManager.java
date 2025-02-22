package de.nvclas.flats.managers;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages a collection of flats and their properties.
 * <p>
 * The {@code FlatsManager} class provides methods for creating, retrieving, updating,
 * and deleting flats, as well as managing their associated areas and owners. It
 * interacts with the configuration system to persist and load data related to the flats.
 */
public class FlatsManager {

    private final Map<String, Flat> allFlats = new HashMap<>();
    private final FlatsConfig config;

    public FlatsManager(Flats flatsPlugin) {
        this.config = flatsPlugin.getFlatsConfig();
        loadAll();
    }

    /**
     * Loads all flat definitions into the manager's internal collection.
     * <p>
     * This method clears the current collection of flats and reloads it from the
     * associated configuration system.
     */
    public void loadAll() {
        allFlats.clear();
        allFlats.putAll(config.loadFlats());
    }

    /**
     * Saves all managed flats to persistent storage using the associated configuration.
     * <p>
     * This method ensures that the current state of all flats is updated in the
     * configuration system, overwriting previous data with the latest changes.
     */
    public void saveAll() {
        config.saveFlats(allFlats);
    }

    /**
     * Retrieves a list of all flat names managed by the {@code FlatsManager}.
     * <p>
     * The returned list is an unmodifiable copy of the flat names currently managed.
     *
     * @return an unmodifiable {@link List} of flat names, where each name is represented as a {@link String}.
     */
    public @NotNull List<String> getAllFlatNames() {
        return List.copyOf(allFlats.keySet());
    }

    /**
     * Retrieves a list of all flats managed by the FlatsManager.
     * <p>
     * The returned list is an unmodifiable copy of the currently managed flats.
     *
     * @return an unmodifiable {@link List} of all {@link Flat} instances.
     */
    public @NotNull List<Flat> getAllFlats() {
        return List.copyOf(allFlats.values());
    }

    /**
     * Retrieves a list of all areas managed across all flats.
     * <p>
     * This method aggregates areas from all available flats managed by the {@link FlatsManager}.
     *
     * @return a {@link List} of all {@link Area} instances currently managed. The list is non-null but may be empty.
     */
    public @NotNull List<Area> getAllAreas() {
        return allFlats.values().stream().flatMap(flat -> flat.getAreas().stream()).toList();
    }

    /**
     * Retrieves the {@link Flat} associated with the given name.
     * <p>
     * If the flat with the specified name does not exist, {@code null} is returned.
     *
     * @param name the name of the flat to retrieve; must not be null.
     * @return the {@link Flat} associated with the specified name, or {@code null} if not found.
     */
    public @Nullable Flat getFlat(@NotNull String name) {
        if (!existsFlat(name)) {
            return null;
        }
        return Objects.requireNonNull(allFlats.get(name),
                "Oops, something went terribly wrong. Please restart the server!");
    }

    /**
     * Retrieves the {@link Flat} associated with the specified {@link Location}.
     * <p>
     * The method searches through all managed flats and determines if the provided
     * {@code location} is within the bounds of any flat.
     *
     * @param location the {@link Location} to check. Must not be null.
     * @return the {@link Flat} containing the specified {@code location}, or {@code null}
     * if no flat is found at the location.
     */
    public @Nullable Flat getFlatByLocation(@NotNull Location location) {
        return allFlats.values()
                .stream()
                .filter(flat -> flat.isWithinBounds(location))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets the owner of the specified flat.
     * <p>
     * This method assigns the given {@link OfflinePlayer} as the owner of the
     * specified {@link Flat}.
     *
     * @param flat  the {@link Flat} whose owner is to be set. Must not be null.
     * @param owner the {@link OfflinePlayer} to be set as the owner. Must not be null.
     */
    public void setOwner(Flat flat, OfflinePlayer owner) {
        flat.setOwner(owner);
    }

    /**
     * Adds a new {@link Area} to an existing flat specified by its name.
     * <p>
     * If no flat exists with the given name, an {@link IllegalArgumentException} is thrown.
     *
     * @param name the name of the flat to which the area should be added; must not be null.
     * @param area the {@link Area} to be added; must not be null.
     * @throws IllegalArgumentException if no flat exists with the given name.
     */
    public void addArea(@NotNull String name, @NotNull Area area) throws IllegalArgumentException {
        if (!existsFlat(name)) {
            throw new IllegalArgumentException("No flat exists with the given name: " + name);
        }
        Flat flat = getFlat(name);
        //noinspection DataFlowIssue
        flat.addArea(area);
    }

    /**
     * Creates a new flat with the specified name and associated area.
     * <p>
     * If a flat with the given name already exists, an {@link IllegalArgumentException} is thrown.
     *
     * @param name the name of the flat to be created; must not be null.
     * @param area the {@link Area} associated with the flat; must not be null.
     * @throws IllegalArgumentException if a flat with the given name already exists.
     */
    public void create(@NotNull String name, @NotNull Area area) throws IllegalArgumentException {
        if (existsFlat(name)) {
            throw new IllegalArgumentException("A flat with this name already exists.");
        }
        Flat newFlat = new Flat(name, area);
        allFlats.put(name, newFlat);
    }

    /**
     * Deletes the flat associated with the given name.
     * <p>
     * If no flat exists with the specified name, an {@link IllegalArgumentException} is thrown.
     *
     * @param name the name of the flat to be deleted. Must not be null.
     * @throws IllegalArgumentException if no flat exists with the given name.
     */
    public void delete(@NotNull String name) throws IllegalArgumentException {
        if (!existsFlat(name)) {
            throw new IllegalArgumentException("No flat exists with the given name: " + name);
        }
        allFlats.remove(name);
    }

    /**
     * Checks if a flat with the specified name exists in the managed collection.
     *
     * @param name the name of the flat to check; must not be null.
     * @return {@code true} if a flat with the given name exists, {@code false} otherwise.
     */
    public boolean existsFlat(@NotNull String name) {
        return allFlats.containsKey(name);
    }

}