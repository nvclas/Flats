package de.nvclas.flats.cache;

import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a spatial index that organizes and queries {@link Flat} objects based on their
 * spatial locations and areas.
 * <p>
 * This class uses a grid-based mapping system to efficiently manage the association
 * between spatial boundaries and {@link Flat} objects.
 */
public class SpatialIndex {

    /**
     * The size of each grid cell in blocks.
     * <p>
     * This value determines the resolution of the spatial grid used by the {@link SpatialIndex}
     * to index and look up flats. Smaller values result in finer granularity of grid cells,
     * potentially increasing memory usage but improving query precision.
     */
    private static final int GRID_SIZE = 16;

    /**
     * A mapping of grid cell coordinates, represented by {@link GridKey}, to
     * the list of {@link Flat} objects that intersect with those cells.
     * <p>
     * This grid-based structure is used to efficiently query and manage flats
     * within specific spatial boundaries.
     */
    private final Map<GridKey, List<Flat>> gridMap = new HashMap<>();

    /**
     * Adds the specified {@link Flat} to the spatial index.
     * <p>
     * The provided flat and its associated areas are integrated into the grid structure
     * to enable efficient location-based queries.
     *
     * @param flat The {@link Flat} to add. Must not be null.
     */
    public void addFlat(@NotNull Flat flat) {
        for (Area area : flat.getAreas()) {
            addAreaToGrid(area, flat);
        }
    }

    /**
     * Removes the specified {@link Flat} from the spatial index.
     * <p>
     * This method ensures that the given {@link Flat} is no longer associated with
     * any grid cells in the index.
     *
     * @param flat The {@link Flat} to be removed. Must not be null.
     */
    public void removeFlat(@NotNull Flat flat) {
        // Remove the flat from all grid cells it might be in
        for (List<Flat> flats : gridMap.values()) {
            flats.remove(flat);
        }

        // Clean up empty lists
        gridMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Removes all entries from the grid, effectively clearing the spatial index.
     * <p>
     * After invoking this method, the index will be empty, and any previously added
     * flats will no longer be tracked.
     */
    public void clear() {
        gridMap.clear();
    }

    /**
     * Retrieves the {@link Flat} that contains the specified {@link Location}, if any.
     * <p>
     * This method searches through the grid cells and evaluates candidate flats
     * to determine whether the given {@link Location} lies within their bounds.
     *
     * @param location The {@link Location} to find the {@link Flat} for. Must not be null.
     * @return The {@link Flat} that contains the specified {@link Location}, or {@code null} if none is found.
     */
    public @Nullable Flat getFlatAtLocation(@NotNull Location location) {
        GridKey key = getGridKey(location);
        List<Flat> candidates = gridMap.getOrDefault(key, List.of());

        for (Flat flat : candidates) {
            if (flat.isWithinBounds(location)) {
                return flat;
            }
        }

        return null;
    }

    /**
     * Adds an {@link Area} of a {@link Flat} to the spatial grid structure.
     * <p>
     * Updates the underlying grid to associate the specified flat with all grid cells
     * that the area intersects.
     *
     * @param area the area to be added to the grid, representing a region of the flat
     * @param flat the flat associated with the area being added
     */
    private void addAreaToGrid(Area area, Flat flat) {
        // Calculate grid cell ranges for this area using the cached boundary values
        int minGridX = (int) Math.floor(area.getMinX() / GRID_SIZE);
        int maxGridX = (int) Math.floor(area.getMaxX() / GRID_SIZE);
        int minGridZ = (int) Math.floor(area.getMinZ() / GRID_SIZE);
        int maxGridZ = (int) Math.floor(area.getMaxZ() / GRID_SIZE);

        // Add the flat to all grid cells this area intersects with
        for (int gridX = minGridX; gridX <= maxGridX; gridX++) {
            for (int gridZ = minGridZ; gridZ <= maxGridZ; gridZ++) {
                GridKey key = new GridKey(gridX, gridZ);
                gridMap.computeIfAbsent(key, k -> new ArrayList<>()).add(flat);
            }
        }
    }

    /**
     * Computes the {@link GridKey} corresponding to the given {@link Location}.
     * <p>
     * The {@link GridKey} identifies which grid cell the location falls into.
     *
     * @param location the {@link Location} for which to compute the grid key
     * @return the {@link GridKey} representing the grid cell containing the given location
     */
    private GridKey getGridKey(Location location) {
        int gridX = location.getBlockX() / GRID_SIZE;
        int gridZ = location.getBlockZ() / GRID_SIZE;
        return new GridKey(gridX, gridZ);
    }

    /**
     * A key for the grid map, representing a grid cell's coordinates.
     */
    private record GridKey(int x, int z) {

    }
}
