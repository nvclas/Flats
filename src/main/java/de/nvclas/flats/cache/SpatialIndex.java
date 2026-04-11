package de.nvclas.flats.cache;

import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
     */
    public static final int GRID_SIZE = 16;
    /**
     * A mapping of grid cell coordinates to the list of {@link FlatArea} objects that intersect with those cells.
     * If a key is present, the cell is considered "loaded". An empty list means no areas intersect the cell.
     * Uses LRU policy to keep memory usage bounded.
     */
    private final Map<GridKey, List<FlatArea>> gridMap = new LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<GridKey, List<FlatArea>> eldest) {
            return size() > 2000;
        }
    };

    /**
     * Adds an {@link Area} to the spatial index.
     *
     * @param area The {@link Area} to add. Must not be null.
     */
    public void addArea(@NotNull Area area) {
        FlatArea flatArea = FlatArea.fromArea(area);
        int minGridX = Math.floorDiv(area.getMinX(), GRID_SIZE);
        int maxGridX = Math.floorDiv(area.getMaxX(), GRID_SIZE);
        int minGridZ = Math.floorDiv(area.getMinZ(), GRID_SIZE);
        int maxGridZ = Math.floorDiv(area.getMaxZ(), GRID_SIZE);

        for (int gridX = minGridX; gridX <= maxGridX; gridX++) {
            for (int gridZ = minGridZ; gridZ <= maxGridZ; gridZ++) {
                GridKey key = new GridKey(gridX, gridZ);
                // Only add if the grid cell is already loaded, otherwise it will be loaded from DB when needed
                if (gridMap.containsKey(key)) {
                    gridMap.get(key).add(flatArea);
                }
            }
        }
    }

    /**
     * Sets the areas for a specific grid cell.
     *
     * @param gridX The grid X coordinate.
     * @param gridZ The grid Z coordinate.
     * @param areas The list of areas in this cell.
     */
    public void setAreas(int gridX, int gridZ, List<Area> areas) {
        GridKey key = new GridKey(gridX, gridZ);
        gridMap.put(key, new ArrayList<>(areas.stream().map(FlatArea::fromArea).toList()));
    }

    /**
     * Checks if a grid cell is loaded.
     *
     * @param location The location to check.
     * @return True if the cell containing the location is loaded.
     */
    public boolean isLoaded(@NotNull Location location) {
        return gridMap.containsKey(getGridKey(location));
    }

    /**
     * Gets the grid key for the given location.
     *
     * @param location The location.
     * @return The grid key.
     */
    public GridKey getGridKey(@NotNull Location location) {
        int gridX = Math.floorDiv(location.getBlockX(), GRID_SIZE);
        int gridZ = Math.floorDiv(location.getBlockZ(), GRID_SIZE);
        return new GridKey(gridX, gridZ);
    }

    /**
     * Removes all areas associated with the specified flat name from the spatial index.
     *
     * @param flatName The name of the flat to be removed. Must not be null.
     */
    public void removeFlat(@NotNull String flatName) {
        for (List<FlatArea> areas : gridMap.values()) {
            areas.removeIf(area -> area.flatName().equals(flatName));
        }

        gridMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Retrieves the name of the flat that contains the specified {@link Location}, if any.
     *
     * @param location The {@link Location} to find the flat name for. Must not be null.
     * @return The name of the flat that contains the specified {@link Location}, or {@code null} if none is found.
     */
    public @Nullable String getFlatNameAtLocation(@NotNull Location location) {
        GridKey key = getGridKey(location);
        List<FlatArea> candidates = gridMap.getOrDefault(key, List.of());

        for (FlatArea area : candidates) {
            if (area.isWithinBounds(location)) {
                return area.flatName();
            }
        }
        return null;
    }

    /**
     * Retrieves the {@link Area} that contains the specified {@link Location}, if any.
     *
     * @param location The {@link Location} to find the {@link Area} for. Must not be null.
     * @return The {@link Area} that contains the specified {@link Location}, or {@code null} if none is found.
     */
    public @Nullable Area getAreaAtLocation(@NotNull Location location) {
        GridKey key = getGridKey(location);
        List<FlatArea> candidates = gridMap.getOrDefault(key, List.of());

        for (FlatArea area : candidates) {
            if (area.isWithinBounds(location)) {
                return area.toArea();
            }
        }
        return null;
    }


    /**
     * Represents a simplified, lightweight version of an {@link Area} for use within the spatial index.
     * Storing these instead of full {@link Flat} objects significantly reduces memory consumption.
     */
    private record FlatArea(String flatName, String worldName, int minX, int maxX, int minY, int maxY, int minZ,
                            int maxZ) {
        public static FlatArea fromArea(Area area) {
            return new FlatArea(area.getFlatName(),
                    area.getPos1().getWorld().getName(),
                    area.getMinX(),
                    area.getMaxX(),
                    area.getMinY(),
                    area.getMaxY(),
                    area.getMinZ(),
                    area.getMaxZ());
        }

        public boolean isWithinBounds(@NotNull Location location) {
            if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
                return false;
            }

            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }

        public @Nullable Area toArea() {
            World world = Bukkit.getWorld(worldName);
            if (world == null)
                return null;
            return new Area(new Location(world, minX, minY, minZ),
                    new Location(world, maxX, maxY, maxZ),
                    flatName);
        }
    }

    /**
     * A key for the grid map, representing a grid cell's coordinates.
     */
    public record GridKey(int x, int z) {

    }
}
