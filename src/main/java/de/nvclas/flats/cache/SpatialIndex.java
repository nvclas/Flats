package de.nvclas.flats.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * A cache of (world, grid x, grid z) to the list of {@link FlatArea} objects that intersect with those cells.
     * If a key is present, the cell is considered "loaded". An empty list means no areas intersect the cell.
     */
    private final Cache<GridKey, List<FlatArea>> gridCache = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    /**
     * Adds an {@link Area} to the spatial index.
     *
     * @param area The {@link Area} to add. Must not be null.
     */
    public void addArea(@NotNull Area area) {
        FlatArea flatArea = FlatArea.fromArea(area);
        String worldName = area.getWorldName();
        int minGridX = Math.floorDiv(area.getMinX(), GRID_SIZE);
        int maxGridX = Math.floorDiv(area.getMaxX(), GRID_SIZE);
        int minGridZ = Math.floorDiv(area.getMinZ(), GRID_SIZE);
        int maxGridZ = Math.floorDiv(area.getMaxZ(), GRID_SIZE);

        for (int gridX = minGridX; gridX <= maxGridX; gridX++) {
            for (int gridZ = minGridZ; gridZ <= maxGridZ; gridZ++) {
                GridKey key = new GridKey(worldName, gridX, gridZ);
                gridCache.asMap().computeIfPresent(key, (k, loadedAreas) -> {
                    List<FlatArea> updated = new ArrayList<>(loadedAreas);
                    updated.add(flatArea);
                    return Collections.unmodifiableList(updated);
                });
            }
        }
    }

    /**
     * Sets the areas for a specific grid cell in a specific world.
     *
     * @param worldName The name of the world.
     * @param gridX     The grid X coordinate.
     * @param gridZ     The grid Z coordinate.
     * @param areas     The list of areas in this cell.
     */
    public void setAreas(@NotNull String worldName, int gridX, int gridZ, @NotNull List<Area> areas) {
        List<FlatArea> flatAreas = new ArrayList<>(areas.size());
        for (Area area : areas) {
            flatAreas.add(FlatArea.fromArea(area));
        }

        gridCache.put(new GridKey(worldName, gridX, gridZ), flatAreas);
    }

    /**
     * Checks if a grid cell identified by the given {@link GridKey} is currently loaded.
     * <p>
     * A grid cell is considered loaded if it has an associated cached entry in the spatial index.
     *
     * @param key The {@link GridKey} identifying the grid cell. Must not be {@code null}.
     * @return {@code true} if the grid cell is loaded, {@code false} otherwise.
     */
    public boolean isLoaded(@NotNull GridKey key) {
        return gridCache.getIfPresent(key) != null;
    }

    /**
     * Checks if a grid cell is loaded.
     *
     * @param location The location to check.
     * @return True if the cell containing the location is loaded.
     */
    public boolean isLoaded(@NotNull Location location) {
        return isLoaded(getGridKey(location));
    }

    /**
     * Gets the grid key for the given location.
     * <p>
     * If the location's world is null, the key uses an empty string for the world name.
     * Such a key will never match a loaded cell (since cells are only loaded via
     * {@link #setAreas(String, int, int, java.util.List)} with a real world name),
     * so null-world locations are always treated as not loaded.
     *
     * @param location The location.
     * @return The grid key, including the world name.
     */
    public GridKey getGridKey(@NotNull Location location) {
        int gridX = Math.floorDiv(location.getBlockX(), GRID_SIZE);
        int gridZ = Math.floorDiv(location.getBlockZ(), GRID_SIZE);
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "";
        return new GridKey(worldName, gridX, gridZ);
    }

    /**
     * Removes all areas associated with the specified flat name from the spatial index.
     *
     * @param flatName The name of the flat to be removed. Must not be null.
     */
    public void removeFlat(@NotNull String flatName) {
        for (GridKey key : gridCache.asMap().keySet()) {
            gridCache.asMap().computeIfPresent(key, (k, loadedAreas) -> loadedAreas.stream()
                    .filter(area -> !area.flatName().equals(flatName))
                    .toList());
        }
    }

    /**
     * Retrieves the name of the flat that contains the specified {@link Location}, if any.
     *
     * @param location The {@link Location} to find the flat name for. Must not be null.
     * @return The name of the flat that contains the specified {@link Location}, or {@code null} if none is found.
     */
    public @Nullable String getFlatNameAtLocation(@NotNull Location location) {
        FlatArea flatArea = getFlatAreaAtLocation(location);
        return flatArea != null ? flatArea.flatName() : null;
    }

    /**
     * Retrieves the {@link Area} that contains the specified {@link Location}, if any.
     *
     * @param location The {@link Location} to find the {@link Area} for. Must not be null.
     * @return The {@link Area} that contains the specified {@link Location}, or {@code null} if none is found.
     */
    public @Nullable Area getAreaAtLocation(@NotNull Location location) {
        FlatArea flatArea = getFlatAreaAtLocation(location);
        return flatArea != null ? flatArea.toArea() : null;
    }

    private @Nullable FlatArea getFlatAreaAtLocation(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        List<FlatArea> candidates = gridCache.getIfPresent(getGridKey(location));
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        for (FlatArea area : candidates) {
            if (area.isWithinBounds(x, y, z)) {
                return area;
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
            return new FlatArea(area.getFlatName(), area.getWorldName(), area.getMinX(), area.getMaxX(), area.getMinY(),
                    area.getMaxY(), area.getMinZ(), area.getMaxZ());
        }

        public boolean isWithinBounds(int x, int y, int z) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }

        public @Nullable Area toArea() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            return new Area(new Location(world, minX, minY, minZ), new Location(world, maxX, maxY, maxZ), flatName);
        }
    }

    /**
     * A key for the grid cache, representing a grid cell's coordinates within a specific world.
     */
    public record GridKey(String worldName, int x, int z) {

    }
}
