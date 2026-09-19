package de.nvclas.flats.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.nvclas.flats.core.events.FlatUpdateEvent;
import de.nvclas.flats.core.storage.StorageAdapter;
import de.nvclas.flats.core.volumes.Area;
import de.nvclas.flats.core.volumes.Flat;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Manages a cache of flats and provides methods to interact with them.
 */
@RequiredArgsConstructor
public class FlatsCache {

    private static final int MAX_CACHED_FLATS = 1_000;
    private static final Duration CACHE_EXPIRATION = Duration.ofMinutes(30);

    private final Cache<String, Flat> cache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHED_FLATS)
            .expireAfterAccess(CACHE_EXPIRATION)
            .build();

    private final SpatialIndex spatialIndex;
    private final GridLoader gridLoader;
    private final StorageAdapter storageAdapter;

    public FlatsCache(@NotNull StorageAdapter storageAdapter) {
        this.storageAdapter = storageAdapter;
        this.spatialIndex = new SpatialIndex();
        this.gridLoader = new GridLoader(storageAdapter, spatialIndex);
    }

    /**
     * Retrieves a {@link Flat} identified by the given name.
     * If the flat is not present in the cache, it attempts to load it from storage.
     * <p>
     * Returns {@code null} if no flat with the specified name exists.
     *
     * @param name The name of the flat to retrieve. Must not be null.
     * @return The {@link Flat} object if found; {@code null} otherwise.
     */
    public @Nullable Flat getFlat(@NotNull String name) {
        String normalizedName = normalizeName(name);
        Flat flat = cache.getIfPresent(normalizedName);
        if (flat != null) {
            return flat;
        }

        flat = storageAdapter.loadFlat(name);
        if (flat != null) {
            cache.put(normalizedName, flat);
        }
        return flat;
    }

    /**
     * Retrieves an existing {@link Flat} instance by its name. This method guarantees the
     * returned {@link Flat} object is not {@code null}. If the flat does not exist, a
     * {@link NullPointerException} is thrown.
     *
     * @param name The name of the {@link Flat} to retrieve. Must not be {@code null}.
     * @return The {@link Flat} object corresponding to the specified name. Never {@code null}.
     * @throws NullPointerException If no {@link Flat} exists with the given name.
     */
    public @NotNull Flat getExistingFlat(@NotNull String name) throws NullPointerException {
        return Objects.requireNonNull(getFlat(name), "Flat '" + name + "' does not exist.");
    }

    /**
     * Retrieves the {@link Flat} located at the specified {@link Location}, if any.
     * <p>
     * The method ensures that relevant grid data is loaded before attempting to locate the flat.
     *
     * @param location The {@link Location} to check for a flat. Must not be null.
     * @return The {@link Flat} associated with the given {@link Location}, or {@code null} if no flat is found.
     */
    public @Nullable Flat getFlatAtLocation(@NotNull Location location) {
        gridLoader.ensureLoaded(location);
        String name = spatialIndex.getFlatNameAtLocation(location);
        return name != null ? getFlat(name) : null;
    }

    /**
     * Retrieves the {@link Area} at the specified {@link Location}, if available.
     * <p>
     * Ensures the relevant data for the location is loaded before querying the spatial index.
     *
     * @param location the {@link Location} for which the associated {@link Area} is to be fetched; must not be null.
     * @return the {@link Area} at the specified {@link Location} if it exists, otherwise {@code null}.
     */
    public @Nullable Area getAreaAtLocation(@NotNull Location location) {
        gridLoader.ensureLoaded(location);
        return spatialIndex.getAreaAtLocation(location);
    }

    /**
     * Checks if the specified location is already loaded in the system.
     * <p>
     * This method determines whether data associated with the given location
     * has been preloaded and is currently accessible in memory.
     *
     * @param location the {@link Location} to check. Must not be {@code null}.
     * @return {@code true} if the location is loaded, {@code false} otherwise.
     */
    public boolean isLocationLoaded(@NotNull Location location) {
        return spatialIndex.isLoaded(spatialIndex.getGridKey(location));
    }

    /**
     * Checks if a flat with the specified name exists, either in the cache or in the storage.
     *
     * @param name The name of the flat to check. Must not be {@code null}. The name is case-insensitive.
     * @return {@code true} if the flat exists; {@code false} otherwise.
     */
    public boolean existsFlat(@NotNull String name) {
        if (cache.getIfPresent(normalizeName(name)) != null) {
            return true;
        }
        return storageAdapter.existsFlat(name);
    }

    /**
     * Creates and registers a new flat with the specified name and area.
     * <p>
     * The flat will be saved in the storage, added to the in-memory cache,
     * and registered within the spatial index. An event indicating the creation
     * of the flat will also be triggered.
     *
     * @param name the unique name of the flat to create; must not be {@code null}.
     * @param area the geographical area associated with the flat; must not be {@code null}.
     * @throws IllegalStateException if a flat with the specified name already exists.
     */
    public void create(@NotNull String name, @NotNull Area area) throws IllegalStateException {
        if (existsFlat(name)) {
            throw new IllegalStateException("A flat with this name already exists.");
        }
        Flat newFlat = new Flat(name, area);
        storageAdapter.saveFlat(newFlat);
        cache.put(normalizeName(name), newFlat);
        spatialIndex.addArea(area);

        fireEvent(newFlat, FlatUpdateEvent.Action.CREATED);
    }

    /**
     * Deletes the specified {@link Flat} from storage, cache, and spatial index.
     * Triggers a {@link FlatUpdateEvent} with the {@link FlatUpdateEvent.Action#DELETED} action.
     *
     * @param flat The {@link Flat} to be deleted. Must not be null.
     */
    public void delete(@NotNull Flat flat) {
        storageAdapter.deleteFlat(flat.getName());
        cache.invalidate(normalizeName(flat.getName()));
        spatialIndex.removeFlat(flat.getName());

        fireEvent(flat, FlatUpdateEvent.Action.DELETED);
    }

    /**
     * Saves the specified {@link Flat} to persistent storage and updates the internal cache.
     * The provided flat must have a valid name and adhere to the system's required state.
     * An {@link FlatUpdateEvent} of type {@link FlatUpdateEvent.Action#UPDATED} is triggered
     * upon successful completion.
     *
     * @param flat The {@link Flat} to be saved. Must not be null.
     */
    public void save(@NotNull Flat flat) {
        storageAdapter.saveFlat(flat);
        cache.put(normalizeName(flat.getName()), flat);

        fireEvent(flat, FlatUpdateEvent.Action.UPDATED);
    }

    /**
     * Adds a new {@link Area} to the specified {@link Flat} and updates relevant caches and spatial indexes.
     *
     * @param flat The {@link Flat} to which the {@link Area} is to be added. Must not be null.
     * @param area The {@link Area} to add to the flat. Must not be null.
     */
    public void addAreaToFlat(@NotNull Flat flat, @NotNull Area area) {
        flat.addArea(area);
        save(flat);
        spatialIndex.addArea(area);
    }

    /**
     * Shuts down the internal resources of the cache, including services used for asynchronous tasks.
     * <p>
     * This method ensures all background operations are terminated gracefully. Any ongoing tasks
     * may be interrupted during shutdown.
     * <p>
     * It is recommended to invoke this method before discarding an instance of the containing class to
     * release system resources effectively.
     */
    public void shutdown() {
        gridLoader.shutdown();
    }

    /**
     * Retrieves a paginated list of flat names.
     * <p>
     * This method fetches a subset of flat names from the storage based on the specified offset and limit parameters.
     * It is useful for implementing pagination in scenarios where the complete list of flat names is too large to process at once.
     *
     * @param offset The number of flat names to skip before starting to collect the result.
     *               This is typically used for paginated displays alongside a limit.
     * @param limit  The maximum number of flat names to retrieve in the current request.
     *               It represents the size of the page for paginated results.
     * @return A non-null list of flat names.
     * The list will contain up to {@code limit} flat names, depending on availability.
     */
    public @NotNull List<String> getPaginatedFlatNames(int offset, int limit) {
        return storageAdapter.getPaginatedFlatNames(offset, limit);
    }

    /**
     * Retrieves the total count of flats currently stored in the cache or backend system.
     *
     * <p>
     * This method delegates the retrieval to the underlying {@link StorageAdapter}.
     *
     * @return The total number of flats across all storage layers.
     */
    public int getTotalFlatsCount() {
        return storageAdapter.getTotalFlatsCount();
    }

    /**
     * Retrieves a list of flat names that start with the specified prefix.
     * <p>
     * The results are limited to the specified maximum number of flat names.
     *
     * @param prefix The prefix used to filter flat names. Must not be null.
     * @param limit  The maximum number of flat names to return. A non-negative integer.
     * @return A list of flat names that match the given prefix. The list will contain at most {@code limit} elements.
     */
    public @NotNull List<String> getFilteredFlatNames(@NotNull String prefix, int limit) {
        return storageAdapter.getFilteredFlatNames(prefix, limit);
    }

    /**
     * Retrieves the total number of flats owned by a specific player.
     *
     * <p>
     * This method queries the storage to calculate the count of flats belonging to the given player.
     *
     * @param player The {@link OfflinePlayer} whose flats are being queried. Must not be null.
     * @return The number of flats owned by the specified player.
     */
    public int getOwnedFlatsCount(@NotNull OfflinePlayer player) {
        return storageAdapter.getOwnedFlatsCount(player);
    }

    /**
     * Preloads grid cells within the specified radius surrounding a location into memory.
     * <p>
     * This method ensures that the relevant grid data for the given location and its surroundings
     * is available, reducing potential delays during later operations that rely on this data.
     *
     * @param location The center {@link Location} around which grid cells should be prefetched; must not be {@code null}.
     * @param radius   The radius (in grid units) within which surrounding grid cells will be prefetched; must be a non-negative integer.
     */
    public void prefetchSurroundingGridCells(@NotNull Location location, int radius) {
        gridLoader.prefetchSurroundingGridCells(location, radius);
    }

    /**
     * Retrieves a list of areas that intersect with the specified boundaries in the given world.
     *
     * @param worldName the name of the world to search in; must not be {@code null}.
     * @param minX      the minimum X-coordinate of the bounding box.
     * @param maxX      the maximum X-coordinate of the bounding box.
     * @param minZ      the minimum Z-coordinate of the bounding box.
     * @param maxZ      the maximum Z-coordinate of the bounding box.
     */
    public @NotNull List<Area> getAreasIntersecting(@NotNull String worldName, int minX, int maxX, int minZ, int maxZ) {
        return storageAdapter.getAreasIntersecting(worldName, minX, maxX, minZ, maxZ);
    }

    private void fireEvent(@NotNull Flat flat, @NotNull FlatUpdateEvent.Action action) {
        new FlatUpdateEvent(flat, action).callEvent();
    }

    private @NotNull String normalizeName(@NotNull String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public @NotNull CompletableFuture<Void> prefetchGridCell(@NotNull String worldName, int gridX, int gridZ) {
        return gridLoader.prefetchGridCell(worldName, gridX, gridZ);
    }
}
