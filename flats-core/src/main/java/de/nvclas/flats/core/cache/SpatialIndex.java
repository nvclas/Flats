package de.nvclas.flats.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import de.nvclas.flats.core.volumes.Area;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpatialIndex {

    public static final int GRID_SIZE = 16;
    private static final int MAX_CACHED_GRIDS = 50_000;
    private static final Duration CACHE_EXPIRATION = Duration.ofMinutes(30);

    private final Cache<GridKey, List<FlatArea>> gridCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHED_GRIDS)
            .expireAfterAccess(CACHE_EXPIRATION)
            .removalListener((GridKey key, List<FlatArea> loadedAreas, RemovalCause cause) -> {
                if (cause != RemovalCause.REPLACED) {
                    removeGridKeyReferences(key, loadedAreas);
                }
            })
            .build();

    private final Map<String, Set<GridKey>> flatGridKeys = new ConcurrentHashMap<>();

    public void setAreas(@NotNull String worldName, int gridX, int gridZ, @NotNull List<Area> areas) {
        List<FlatArea> flatAreas = new ArrayList<>(areas.size());
        GridKey key = new GridKey(worldName, gridX, gridZ);
        for (Area area : areas) {
            FlatArea flatArea = FlatArea.fromArea(area);
            flatAreas.add(flatArea);
            flatGridKeys.computeIfAbsent(flatArea.flatName(), k -> ConcurrentHashMap.newKeySet()).add(key);
        }

        gridCache.put(key, List.copyOf(flatAreas));
    }

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
                boolean[] wasPresent = {false};
                gridCache.asMap().computeIfPresent(key, (k, loadedAreas) -> {
                    wasPresent[0] = true;
                    List<FlatArea> updated = new ArrayList<>(loadedAreas);
                    updated.add(flatArea);
                    return Collections.unmodifiableList(updated);
                });
                if (wasPresent[0]) {
                    flatGridKeys.computeIfAbsent(flatArea.flatName(), k -> ConcurrentHashMap.newKeySet()).add(key);
                }
            }
        }
    }

    public void removeFlat(@NotNull String flatName) {
        Set<GridKey> keys = flatGridKeys.remove(flatName);
        if (keys == null) {
            return;
        }
        for (GridKey key : keys) {
            gridCache.asMap()
                    .computeIfPresent(key, (k, loadedAreas) -> loadedAreas.stream()
                            .filter(area -> !area.flatName().equals(flatName))
                            .toList());
        }
    }

    public boolean isLoaded(@NotNull GridKey key) {
        return gridCache.getIfPresent(key) != null;
    }

    public @NotNull GridKey getGridKey(@NotNull Location location) {
        int gridX = Math.floorDiv(location.getBlockX(), GRID_SIZE);
        int gridZ = Math.floorDiv(location.getBlockZ(), GRID_SIZE);
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "";
        return new GridKey(worldName, gridX, gridZ);
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

    private void removeGridKeyReferences(@NotNull GridKey key, @NotNull List<FlatArea> loadedAreas) {
        for (FlatArea area : loadedAreas) {
            flatGridKeys.computeIfPresent(area.flatName(), (flatName, keys) -> {
                keys.remove(key);
                return keys.isEmpty() ? null : keys;
            });
        }
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
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
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
