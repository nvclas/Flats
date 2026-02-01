package de.nvclas.flats.cache;

import de.nvclas.flats.storage.FlatsStorage;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages a cache of flats and provides methods to interact with them.
 *
 * <p>
 * The {@code FlatsCache} class handles the storage and operations related to
 * flat management, such as adding, retrieving, and deleting flats.
 * It uses a SQLite database to persist data and a spatial index for efficient queries.
 */
public class FlatsCache {

    /**
     * An LRU cache to store {@link Flat} objects in memory.
     * This prevents loading all flats into RAM while keeping frequently used ones accessible.
     */
    private final Map<String, Flat> flatCache = new LinkedHashMap<>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Flat> eldest) {
            return size() > 1000;
        }
    };

    private final FlatsStorage flatsStorage;
    private final SpatialIndex spatialIndex = new SpatialIndex();

    public FlatsCache(FlatsStorage flatsStorage) {
        this.flatsStorage = flatsStorage;
        loadAll();
    }

    /**
     * Loads the spatial index from the database.
     * Full flat objects are not loaded into memory until requested.
     */
    public void loadAll() {
        flatCache.clear();
        spatialIndex.clear();

        List<Area> allAreas = flatsStorage.loadAllAreas();
        for (Area area : allAreas) {
            spatialIndex.addArea(area);
        }
    }

    /**
     * No longer used as data is saved on every change.
     */
    public void saveAll() {
        // Ignored in new architecture
    }

    /**
     * Retrieves a list of all flat names currently available in the database.
     *
     * @return a {@link List} of flat names. Never {@code null}.
     */
    public @NotNull List<String> getAllFlatNames() {
        return flatsStorage.getAllFlatNames();
    }

    /**
     * Retrieves a list of all flats.
     * <p>
     * WARNING: This method will load all flats from the database into the cache.
     * Use sparingly.
     *
     * @return an unmodifiable {@link List} containing all {@link Flat} instances.
     */
    public @NotNull List<Flat> getAllFlats() {
        return getAllFlatNames().stream()
                .map(this::getFlat)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Retrieves all {@link Area} instances associated with all flats.
     *
     * @return A non-null {@link List} of {@link Area} instances.
     */
    public @NotNull List<Area> getAllAreas() {
        return spatialIndex.getAllAreas();
    }

    /**
     * Retrieves a {@link Flat} by its name.
     * <p>
     * Attempts to retrieve from the LRU cache first, otherwise loads from the database.
     * Returns null if no flat with the specified name exists.
     *
     * @param name The name of the flat to retrieve. Must not be null.
     * @return The {@link Flat} with the given name, or {@code null} if no such flat exists.
     */
    public @Nullable Flat getFlat(@NotNull String name) {
        Flat flat = flatCache.get(name);
        if (flat == null) {
            flat = flatsStorage.loadFlat(name);
            if (flat != null) {
                flatCache.put(name, flat);
            }
        }
        return flat;
    }

    /**
     * Retrieves an existing {@link Flat} by its name.
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
     *
     * @param location the {@link Location} to find a flat for. Must not be {@code null}.
     * @return the {@link Flat} containing the specified location, or {@code null} if no flat contains the location.
     */
    public @Nullable Flat getFlatByLocation(@NotNull Location location) {
        String name = spatialIndex.getFlatNameAtLocation(location);
        return name != null ? getFlat(name) : null;
    }

    public @Nullable Area getAreaAtLocation(@NotNull Location location) {
        return spatialIndex.getAreaAtLocation(location);
    }

    /**
     * Retrieves the number of flats owned by the specified player.
     *
     * @param player The {@link OfflinePlayer} whose owned flats are to be counted. Must not be {@code null}.
     * @return The number of flats owned by the specified player.
     */
    public int getOwnedFlatsCount(@NotNull OfflinePlayer player) {
        return flatsStorage.getOwnedFlatsCount(player);
    }

    /**
     * Creates a new flat with the specified name and area.
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
        flatsStorage.saveFlat(newFlat);
        flatCache.put(name, newFlat);
        spatialIndex.addArea(area);
    }

    /**
     * Deletes the specified flat by its name.
     *
     * @param name the name of the flat to delete; must not be {@code null}.
     * @throws IllegalStateException if no flat with the specified name exists.
     */
    public void delete(@NotNull String name) throws IllegalStateException {
        if (!existsFlat(name)) {
            throw new IllegalStateException("No flat exists with the given name: " + name);
        }
        flatsStorage.deleteFlat(name);
        flatCache.remove(name);
        spatialIndex.removeFlat(name);
    }

    /**
     * Checks if a flat with the specified name exists.
     *
     * @param name the name of the flat to check; must not be null.
     * @return {@code true} if a flat with the given name exists, {@code false} otherwise.
     */
    public boolean existsFlat(@NotNull String name) {
        return getAllFlatNames().contains(name);
    }

    /**
     * Saves the current state of a flat to the database.
     *
     * @param flat The flat to save. Must not be null.
     */
    public void save(@NotNull Flat flat) {
        flatsStorage.saveFlat(flat);
    }

    /**
     * Adds an area to an existing flat and persists the changes.
     *
     * @param flat The flat to add the area to.
     * @param area The area to add.
     */
    public void addAreaToFlat(@NotNull Flat flat, @NotNull Area area) {
        flat.addArea(area);
        save(flat);
        spatialIndex.addArea(area);
    }
}
