package de.nvclas.flats.core.cache;

import de.nvclas.flats.core.storage.StorageAdapter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class GridLoader {

    private final StorageAdapter storageAdapter;
    private final SpatialIndex spatialIndex;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Flats-DB-Worker");
        thread.setDaemon(true);
        return thread;
    });

    private final ConcurrentHashMap<SpatialIndex.GridKey, CompletableFuture<Void>> loadingFutures = new ConcurrentHashMap<>();

    /**
     * Initiates asynchronous loading of grid cell data for the specified world and coordinates.
     * If the grid cell is already loaded, a completed future is returned immediately.
     * The loaded data is stored in the spatial index for future use.
     *
     * @param worldName the name of the world
     * @param gridX     the X coordinate of the grid cell
     * @param gridZ     the Z coordinate of the grid cell
     * @return a {@link CompletableFuture} that completes when the grid cell data is loaded
     */
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

    /**
     * Initiates asynchronous loading of grid cell data for a square region surrounding the specified location.
     * <p>
     * The method calculates the grid coordinates based on the provided location and loads all cells
     * within the defined radius in both X and Z directions.
     *
     * @param location the central {@code Location} used to determine the starting grid cell.
     * @param radius   the distance (in grid units) from the center to the farthest grid cell to be loaded.
     */
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

    /**
     * Initiates the loading of the grid cell data corresponding to the provided location.
     * <p>
     * If the data is already loaded, this method does nothing. If it is not loaded, the data is asynchronously
     * preloaded and the method waits until the loading process is complete.
     *
     * @param location the {@code Location} used to determine the grid cell that needs to be loaded.
     */
    public void ensureLoaded(@NotNull Location location) {
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

    /**
     * Shuts down the background data loading executor and attempts to gracefully terminate all ongoing tasks.
     * <p>
     * If the executor does not terminate within a specified timeout, it forces immediate shutdown.
     * <p>
     * If the current thread is interrupted during shutdown, it forces immediate shutdown and re-interrupts the current thread.
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
}
