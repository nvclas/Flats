package de.nvclas.flats.cache;

import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private static final int GRID_SIZE = 16;
    /**
     * A mapping of grid cell coordinates to the list of {@link FlatArea} objects that intersect with those cells.
     */
    private final Map<GridKey, List<FlatArea>> gridMap = HashMap.newHashMap(1024);

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
                gridMap.computeIfAbsent(key, k -> new ArrayList<>()).add(flatArea);
            }
        }
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
     * Removes all entries from the grid, effectively clearing the spatial index.
     */
    public void clear() {
        gridMap.clear();
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
     * Retrieves all areas currently indexed.
     *
     * @return A list of all {@link Area} objects.
     */
    public @NotNull List<Area> getAllAreas() {
        return gridMap.values().stream()
                .flatMap(List::stream)
                .distinct()
                .map(FlatArea::toArea)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Computes the {@link GridKey} corresponding to the grid cell that contains the specified {@link Location}.
     *
     * @param location The {@link Location} for which the grid key is to be generated. Must not be null.
     * @return A {@link GridKey} representing the grid cell coordinates for the specified {@link Location}.
     */
    private GridKey getGridKey(Location location) {
        int gridX = Math.floorDiv(location.getBlockX(), GRID_SIZE);
        int gridZ = Math.floorDiv(location.getBlockZ(), GRID_SIZE);
        return new GridKey(gridX, gridZ);
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
    private record GridKey(int x, int z) {

    }
}
