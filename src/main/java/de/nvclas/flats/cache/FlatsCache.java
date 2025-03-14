package de.nvclas.flats.cache;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages a cache of flats and provides methods to interact with them.
 *
 * <p>
 * The {@code FlatsCache} class handles the storage and operations related to 
 * flat management, such as adding, retrieving, saving, and deleting flats. 
 * It uses a configuration source to persist and load data.
 *
 * <p>
 * Instances of this class are initialized with a reference to the 
 * {@link FlatsConfig}, enabling seamless integration with the underlying storage.
 */
public class FlatsCache {

    private final Map<String, Flat> allFlats = new HashMap<>();
    private final FlatsConfig config;

    public FlatsCache(Flats flatsPlugin) {
        this.config = flatsPlugin.getFlatsConfig();
        loadAll();
    }

    /**
     * Loads all flats into the cache by clearing the current data and reloading it from the configuration.
     * <p>
     * This method ensures that the cached flat data is synchronized with the data stored in the configuration source.
     */
    public void loadAll() {
        allFlats.clear();
        allFlats.putAll(config.loadFlats());
    }

    /**
     * Saves all flats managed by this cache to the underlying configuration.
     *
     * <p>
     * This method ensures that the current state of all flats is persisted,
     * allowing changes to be safely stored and restored in future sessions.
     *
     * <p>
     * Utilizes the associated {@link FlatsConfig} instance to handle the persistence.
     *
     * @throws IllegalStateException if an error occurs during the save process.
     */
    public void saveAll() {
        config.saveFlats(allFlats);
    }

    /**
     * Retrieves a list of all flat names currently available in the cache.
     *
     * <p>
     * The returned list is unmodifiable, ensuring that operations on this list
     * do not affect the internal state of the cache.
     *
     * @return an unmodifiable {@link List} of flat names. Never {@code null}.
     */
    public @NotNull List<String> getAllFlatNames() {
        return List.copyOf(allFlats.keySet());
    }

    /**
     * Retrieves a list of all flats currently stored in the cache.
     *
     * <p>
     * The returned list is immutable and contains all the {@link Flat} objects
     * managed by the cache. Modifications to the returned list are not allowed.
     *
     * @return an unmodifiable {@link List} containing all {@link Flat} instances in the cache.
     */
    public @NotNull List<Flat> getAllFlats() {
        return List.copyOf(allFlats.values());
    }

    /**
     * Retrieves all {@link Area} instances associated with all flats managed by this cache.
     *
     * <p>
     * The method aggregates areas from all flats stored within the cache and returns
     * them as a single list.
     *
     * @return A non-null {@link List} of {@link Area} instances representing all areas.
     *         The returned list may be empty if no areas are defined.
     */
    public @NotNull List<Area> getAllAreas() {
        return allFlats.values().stream().flatMap(flat -> flat.getAreas().stream()).toList();
    }

    /**
     * Retrieves a {@link Flat} by its name.
     * <p>
     * Returns null if no flat with the specified name exists.
     *
     * @param name The name of the flat to retrieve. Must not be null.
     * @return The {@link Flat} with the given name, or {@code null} if no such flat exists.
     */
    public @Nullable Flat getFlat(@NotNull String name) {
        if (!existsFlat(name)) {
            return null;
        }
        return Objects.requireNonNull(allFlats.get(name));
    }

    /**
     * Retrieves an existing {@link Flat} by its name.
     * <p>
     * If no flat with the specified name exists, a {@link NullPointerException} is thrown.
     *
     * @param name The name of the flat to retrieve. Must not be null.
     * @return The {@link Flat} corresponding to the provided name.
     * @throws NullPointerException If no flat with the specified name exists.
     */
    public @NotNull Flat getExistingFlat(@NotNull String name) throws NullPointerException {
        return Objects.requireNonNull(getFlat(name), "Flat '" + name + "' does not exist.");
    }

    /**
     * Retrieves the {@link Flat} that contains the provided {@link Location}.
     * <p>
     * The method checks all existing flats and returns the first one where the location 
     * is within the bounds of any associated {@link Area}. If no such flat is found, it returns {@code null}.
     *
     * @param location the {@link Location} to find a flat for. Must not be {@code null}.
     * @return the {@link Flat} containing the specified location, or {@code null} if no flat contains the location.
     */
    public @Nullable Flat getFlatByLocation(@NotNull Location location) {
        return allFlats.values().stream().filter(flat -> flat.isWithinBounds(location)).findFirst().orElse(null);
    }
    
    /**
     * Creates a new flat with the specified name and area.
     *
     * <p>
     * The created flat is stored in the internal cache. If a flat with the 
     * same name already exists, an exception is thrown.
     *
     * @param name the name of the flat to be created, must not be null
     * @param area the area of the flat to be created, must not be null
     * @throws IllegalStateException if a flat with the specified name already exists
     */
    public void create(@NotNull String name, @NotNull Area area) throws IllegalStateException {
        if (existsFlat(name)) {
            throw new IllegalStateException("A flat with this name already exists.");
        }
        Flat newFlat = new Flat(name, area);
        allFlats.put(name, newFlat);
    }

    /**
     * Deletes the specified flat by its name.
     *
     * <p>Removes the flat from the underlying cache if it exists.
     *
     * @param name the name of the flat to delete; must not be {@code null}.
     * @throws IllegalStateException if no flat with the specified name exists.
     */
    public void delete(@NotNull String name) throws IllegalStateException {
        if (!existsFlat(name)) {
            throw new IllegalStateException("No flat exists with the given name: " + name);
        }
        allFlats.remove(name);
    }

    /**
     * Checks if a flat with the specified name exists in the cache.
     *
     * @param name the name of the flat to check; must not be null.
     * @return {@code true} if a flat with the given name exists, {@code false} otherwise.
     */
    public boolean existsFlat(@NotNull String name) {
        return allFlats.containsKey(name);
    }

}