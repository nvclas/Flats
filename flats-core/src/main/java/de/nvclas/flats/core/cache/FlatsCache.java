package de.nvclas.flats.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.nvclas.flats.core.storage.StorageAdapter;
import de.nvclas.flats.core.volumes.Area;
import de.nvclas.flats.core.volumes.Flat;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Flats-DB-Worker");
        thread.setDaemon(true);
        return thread;
    });

    private final ConcurrentHashMap<SpatialIndex.GridKey, CompletableFuture<Void>> loadingFutures = new ConcurrentHashMap<>();

    private final StorageAdapter storageAdapter;
    private final SpatialIndex spatialIndex = new SpatialIndex();

    public @NotNull CompletableFuture<Void> prefetchGridCell(@NotNull String worldName, int gridX, int gridZ) {
        SpatialIndex.GridKey key = new SpatialIndex.GridKey(worldName, gridX, gridZ);
        if (spatialIndex.isLoaded(key)) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = loadingFutures.computeIfAbsent(key, k -> {
            int minX = gridX * SpatialIndex.GRID_SIZE;
            int maxX = minX + SpatialIndex.GRID_SIZE - 1;
            int minZ = gridZ * SpatialIndex.GRID_SIZE;
            int maxZ = minZ + SpatialIndex.GRID_SIZE - 1;

            return CompletableFuture.supplyAsync(
                            () -> storageAdapter.getAreasIntersecting(worldName, minX, maxX, minZ, maxZ), dbExecutor)
                    .thenAccept(areas -> spatialIndex.setAreas(worldName, gridX, gridZ, areas));
        });

        future.whenComplete((res, ex) -> loadingFutures.remove(key, future));
        return future;
    }

    public void prefetchSurroundingGridCells(@NotNull Location location, int radius) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        int centerGridX = Math.floorDiv(location.getBlockX(), SpatialIndex.GRID_SIZE);
        int centerGridZ = Math.floorDiv(location.getBlockZ(), SpatialIndex.GRID_SIZE);
        String worldName = world.getName();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                prefetchGridCell(worldName, centerGridX + dx, centerGridZ + dz);
            }
        }
    }

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

    public @NotNull List<String> getPaginatedFlatNames(int offset, int limit) {
        return storageAdapter.getPaginatedFlatNames(offset, limit);
    }

    public int getTotalFlatsCount() {
        return storageAdapter.getTotalFlatsCount();
    }

    public @NotNull List<String> getFilteredFlatNames(@NotNull String prefix, int limit) {
        return storageAdapter.getFilteredFlatNames(prefix, limit);
    }

    public @NotNull List<Area> getAreasIntersecting(@NotNull String worldName, int minX, int maxX, int minZ, int maxZ) {
        return storageAdapter.getAreasIntersecting(worldName, minX, maxX, minZ, maxZ);
    }

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

    public @NotNull Flat getExistingFlat(@NotNull String name) throws NullPointerException {
        return Objects.requireNonNull(getFlat(name), "Flat '" + name + "' does not exist.");
    }

    public @Nullable Flat getFlatAtLocation(@NotNull Location location) {
        ensureLoaded(location);
        String name = spatialIndex.getFlatNameAtLocation(location);
        return name != null ? getFlat(name) : null;
    }

    public @Nullable Area getAreaAtLocation(@NotNull Location location) {
        ensureLoaded(location);
        return spatialIndex.getAreaAtLocation(location);
    }

    public boolean isLocationLoaded(@NotNull Location location) {
        return spatialIndex.isLoaded(spatialIndex.getGridKey(location));
    }

    private void ensureLoaded(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        SpatialIndex.GridKey key = spatialIndex.getGridKey(location);
        if (spatialIndex.isLoaded(key)) {
            return;
        }
        CompletableFuture<Void> future = prefetchGridCell(world.getName(), key.x(), key.z());
        try {
            future.join();
        } catch (CancellationException | CompletionException ignored) {
            // ignored
        }
    }

    private @NotNull String normalizeName(@NotNull String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public int getOwnedFlatsCount(@NotNull OfflinePlayer player) {
        return storageAdapter.getOwnedFlatsCount(player);
    }

    public void create(@NotNull String name, @NotNull Area area) throws IllegalStateException {
        if (existsFlat(name)) {
            throw new IllegalStateException("A flat with this name already exists.");
        }
        Flat newFlat = new Flat(name, area);
        storageAdapter.saveFlat(newFlat);
        cache.put(normalizeName(name), newFlat);
        spatialIndex.addArea(area);
    }

    public void delete(@NotNull Flat flat) throws IllegalStateException {
        storageAdapter.deleteFlat(flat.getName());
        cache.invalidate(normalizeName(flat.getName()));
        spatialIndex.removeFlat(flat.getName());
    }

    public boolean existsFlat(@NotNull String name) {
        if (cache.getIfPresent(normalizeName(name)) != null) {
            return true;
        }
        return storageAdapter.existsFlat(name);
    }

    public void save(@NotNull Flat flat) {
        storageAdapter.saveFlat(flat);
        cache.put(normalizeName(flat.getName()), flat);
    }

    public void addAreaToFlat(@NotNull Flat flat, @NotNull Area area) {
        flat.addArea(area);
        save(flat);
        spatialIndex.addArea(area);
    }
}
