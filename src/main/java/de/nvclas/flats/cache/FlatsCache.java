package de.nvclas.flats.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.nvclas.flats.storage.FlatsStorage;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
     * A cache for storing and retrieving {@link Flat} objects by their names.
     * <p>
     * This cache is configured with a maximum size of 1,000 entries and an expiration policy
     * that removes entries if they have not been accessed within 30 minutes.
     * <p>
     * Used to optimize retrieval of frequently accessed flats and reduce direct access to storage.
     */
    private final Cache<String, Flat> flatCache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    /**
     * Executor service dedicated to handling database-related tasks in a single-threaded context.
     * <p>
     * Uses a daemon thread named {@code Flats-DB-Worker} to ensure that database operations
     * are executed sequentially, preventing concurrency issues and maintaining thread safety.
     * <p>
     * This executor is intended for internal use within the {@link FlatsCache} class to handle
     * asynchronous operations such as prefetching and database querying.
     * <p>
     * Thread management and cleanup are automatically handled upon JVM shutdown due to the
     * daemon thread configuration.
     *
     * @see ExecutorService
     * @see Executors#newSingleThreadExecutor(java.util.concurrent.ThreadFactory)
     */
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Flats-DB-Worker");
        thread.setDaemon(true);
        return thread;
    });

    private final Set<SpatialIndex.GridKey> loadingCells = ConcurrentHashMap.newKeySet();

    private final FlatsStorage flatsStorage;
    private final SpatialIndex spatialIndex = new SpatialIndex();

    public FlatsCache(FlatsStorage flatsStorage) {
        this.flatsStorage = flatsStorage;
    }

    /**
     * Initiates asynchronous loading of the specified grid cell, fetching intersecting areas and updating the spatial index.
     * <p>
     * If the cell is already loaded or is in the process of being loaded, the method returns immediately without additional action.
     *
     * @param worldName The name of the world to which the grid cell belongs. Must not be {@code null}.
     * @param gridX     The X-coordinate of the grid cell.
     * @param gridZ     The Z-coordinate of the grid cell.
     */
    public void prefetchGridCell(@NotNull String worldName, int gridX, int gridZ) {
        SpatialIndex.GridKey key = new SpatialIndex.GridKey(worldName, gridX, gridZ);
        if (spatialIndex.isLoaded(key) || !loadingCells.add(key)) {
            return;
        }

        int minX = gridX * SpatialIndex.GRID_SIZE;
        int maxX = minX + SpatialIndex.GRID_SIZE - 1;
        int minZ = gridZ * SpatialIndex.GRID_SIZE;
        int maxZ = minZ + SpatialIndex.GRID_SIZE - 1;

        CompletableFuture.supplyAsync(() -> flatsStorage.getAreasIntersecting(worldName, minX, maxX, minZ, maxZ),
                dbExecutor).whenComplete((areas, throwable) -> {
            loadingCells.remove(key);
            if (throwable == null) {
                spatialIndex.setAreas(worldName, gridX, gridZ, areas);
            }
        });
    }

    /**
     * Initiates an orderly shutdown of the underlying executor service used by this component.
     * <p>
     * Ongoing tasks will be allowed to complete within a specified timeout period. If tasks
     * do not terminate within the timeout, a forced shutdown will be attempted.
     * <p>
     * This method ensures proper resource cleanup and should be called to terminate the service
     * when it is no longer needed.
     */
    public void shutdown() {
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Retrieves a paginated list of flat names from the storage.
     * <p>
     * The list is constrained by the specified offset and limit.
     *
     * @param offset The number of entries to skip before starting the retrieval.
     * @param limit  The maximum number of flat names to retrieve.
     * @return A non-null list of flat names, which may be empty if no results are found.
     */
    public @NotNull List<String> getPaginatedFlatNames(int offset, int limit) {
        return flatsStorage.getPaginatedFlatNames(offset, limit);
    }

    /**
     * Retrieves the total number of flats currently stored.
     *
     * <p>
     * Delegates the call to {@link FlatsStorage#getTotalFlatsCount()} and returns the result.
     *
     * @return The total count of flats as an integer.
     */
    public int getTotalFlatsCount() {
        return flatsStorage.getTotalFlatsCount();
    }

    /**
     * Retrieves a list of flat names that begin with the specified prefix, up to the given limit.
     *
     * <p>
     * Filters flat names based on the provided prefix and limits the number of results returned.
     *
     * @param prefix The prefix used to filter flat names. Cannot be {@code null}.
     * @param limit  The maximum number of flat names to include in the result. Should be a positive integer.
     * @return A {@link List} of flat names that match the provided prefix. An empty list is returned if no matches are found.
     */
    public @NotNull List<String> getFilteredFlatNames(@NotNull String prefix, int limit) {
        return flatsStorage.getFilteredFlatNames(prefix, limit);
    }

    /**
     * Retrieves a list of areas that intersect with the specified rectangular boundary in the given world.
     *
     * @param worldName the name of the world where the areas are located.
     * @param minX      the minimum X-coordinate of the boundary.
     * @param maxX      the maximum X-coordinate of the boundary.
     * @param minZ      the minimum Z-coordinate of the boundary.
     * @param maxZ      the maximum Z-coordinate of the boundary.
     * @return a list of {@link Area} objects that intersect with the specified boundary; an empty list if no intersection is found.
     */
    public @NotNull List<Area> getAreasIntersecting(@NotNull String worldName, int minX, int maxX, int minZ, int maxZ) {
        return flatsStorage.getAreasIntersecting(worldName, minX, maxX, minZ, maxZ);
    }


    /**
     * Retrieves the {@link Flat} associated with the specified name.
     * <p>
     * If the flat is not found in the cache, it attempts to load it from persistent storage.
     *
     * @param name The name of the flat to retrieve. Must not be {@code null}.
     * @return The {@link Flat} associated with the specified name, or {@code null} if no such flat exists.
     */
    public @Nullable Flat getFlat(@NotNull String name) {
        Flat flat = flatCache.getIfPresent(name);
        if (flat != null) {
            return flat;
        }

        flat = flatsStorage.loadFlat(name);
        if (flat != null) {
            flatCache.put(name, flat);
        }
        return flat;
    }

    /**
     * Retrieves an existing {@link Flat} by its name.
     * <p>
     * If no flat with the given name exists, a {@link NullPointerException} will be thrown.
     *
     * @param name The name of the flat to retrieve. Must not be null.
     * @return The existing {@link Flat} with the specified name. Never null.
     * @throws NullPointerException If no flat with the given name exists or the name is null.
     */
    public @NotNull Flat getExistingFlat(@NotNull String name) throws NullPointerException {
        return Objects.requireNonNull(getFlat(name), "Flat '" + name + "' does not exist.");
    }

    /**
     * Retrieves a {@link Flat} located at the specified {@link Location}, if one exists.
     * <p>
     * The method determines whether a flat can be found at the provided location
     * and, if so, returns the corresponding {@link Flat} object. If no flat is found,
     * the result will be {@code null}.
     *
     * @param location The {@link Location} to search for a flat. Must not be null.
     * @return The {@link Flat} located at the given {@link Location}, or {@code null} if none exists.
     */
    public @Nullable Flat getFlatAtLocation(@NotNull Location location) {
        ensureLoaded(location);
        String name = spatialIndex.getFlatNameAtLocation(location);
        return name != null ? getFlat(name) : null;
    }

    /**
     * Retrieves the {@link Area} at the specified {@link Location}, if any exists.
     * <p>
     * The location is validated and dynamically loaded if necessary before querying.
     *
     * @param location The {@link Location} to query. Must not be null.
     * @return The {@link Area} at the specified {@link Location}, or {@code null} if no area exists there.
     */
    public @Nullable Area getAreaAtLocation(@NotNull Location location) {
        ensureLoaded(location);
        return spatialIndex.getAreaAtLocation(location);
    }

    /**
     * Ensures that the spatial grid cell associated with the given location is loaded.
     * If the grid cell is not loaded, it is prefetched.
     *
     * @param location the {@link Location} whose associated grid cell is to be checked and loaded if necessary. Must not be {@code null}.
     */
    private void ensureLoaded(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        SpatialIndex.GridKey key = spatialIndex.getGridKey(location);
        if (!spatialIndex.isLoaded(key)) {
            prefetchGridCell(world.getName(), key.x(), key.z());
        }
    }

    /**
     * Retrieves the total count of flats owned by the specified player.
     *
     * <p>
     * This method provides the number of flats associated with the given player's unique identifier.
     *
     * @param player The {@link OfflinePlayer} whose owned flats are to be counted. Must not be {@code null}.
     * @return The total number of flats owned by the specified player. Returns {@code 0} if the player owns no flats.
     */
    public int getOwnedFlatsCount(@NotNull OfflinePlayer player) {
        return flatsStorage.getOwnedFlatsCount(player);
    }

    /**
     * Creates a new flat with the specified name and area, storing it in the system.
     * <p>
     * Throws an exception if a flat with the given name already exists.
     *
     * @param name the unique name of the flat to be created. Must not be {@code null}.
     * @param area the geographical area associated with the flat. Must not be {@code null}.
     * @throws IllegalStateException if a flat with the specified name already exists.
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
     * Deletes a flat with the specified name.
     * <p>
     * This method removes the flat from persistent storage, invalidates its cache entry, and
     * updates the spatial index to reflect the deletion.
     *
     * @param name the name of the flat to delete; must not be {@code null}.
     * @throws IllegalStateException if no flat exists with the given name.
     */
    public void delete(@NotNull String name) throws IllegalStateException {
        if (!existsFlat(name)) {
            throw new IllegalStateException("No flat exists with the given name: " + name);
        }
        flatsStorage.deleteFlat(name);
        flatCache.invalidate(name);
        spatialIndex.removeFlat(name);
    }

    /**
     * Checks whether a flat with the specified name exists.
     * <p>
     * This method verifies the presence of a flat by first checking the cache
     * and, if necessary, querying the underlying storage.
     *
     * @param name The name of the flat to check; must not be {@code null}.
     * @return {@code true} if a flat with the given name exists, {@code false} otherwise.
     */
    public boolean existsFlat(@NotNull String name) {
        if (flatCache.getIfPresent(name) != null) {
            return true;
        }
        return flatsStorage.existsFlat(name);
    }

    /**
     * Saves the given {@link Flat} to persistent storage and updates the in-memory cache.
     * <p>
     * This method ensures that the flat's data is persisted using {@link FlatsStorage#saveFlat(Flat)}
     * and updates the cache entry for faster access.
     *
     * @param flat The {@link Flat} to be saved. Must not be null.
     */
    public void save(@NotNull Flat flat) {
        flatsStorage.saveFlat(flat);
        flatCache.put(flat.getName(), flat);
    }

    /**
     * Adds a new {@link Area} to the specified {@link Flat} and updates the relevant spatial and storage indices.
     *
     * <p>
     * This method associates the provided {@code Area} with the given {@code Flat} by adding it to the flat's area list.
     * Additionally, it ensures that the flat and area changes are persisted in the appropriate storage and spatial index.
     *
     * @param flat The {@link Flat} to which the {@link Area} is to be added. Must not be null.
     * @param area The {@link Area} to add to the {@link Flat}. Must not be null.
     */
    public void addAreaToFlat(@NotNull Flat flat, @NotNull Area area) {
        flat.addArea(area);
        save(flat);
        spatialIndex.addArea(area);
    }
}
