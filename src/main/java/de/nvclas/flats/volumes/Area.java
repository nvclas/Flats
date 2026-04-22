package de.nvclas.flats.volumes;

import de.nvclas.flats.util.LocationConverter;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a three-dimensional area defined by two corner points and a designated name.
 * This class provides functionality for area creation, boundary checks, and interacting
 * with its outer block structure.
 */
@Getter
public class Area {

    private final Location pos1;
    private final Location pos2;
    private final String flatName;
    private final String worldName;
    private final String locationString;

    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;

    /**
     * Constructs a new {@code Area} with the specified corner positions and flat name.
     * <p>
     * This constructor initializes an area defined by two corner points and associates it
     * with a specific flat. It also pre-calculates and caches the minimum and maximum
     * coordinate values for each dimension (X, Y, Z) to optimize boundary checks.
     * <p>
     * Both {@code pos1} and {@code pos2} must reference a loaded world; use
     * {@link #fromRawData(String, Bounds, String)} when the world
     * may not be currently loaded.
     *
     * @param pos1     The first corner position of the area. Must not be null, and its world must not be null.
     * @param pos2     The second corner position of the area. Must not be null.
     * @param flatName The name of the flat this area belongs to. Must not be null.
     */
    public Area(Location pos1, Location pos2, String flatName) {
        Bounds bounds = Bounds.fromLocations(pos1, pos2);
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.flatName = flatName;
        this.worldName = pos1.getWorld().getName();
        this.locationString = LocationConverter.getStringFromLocations(pos1, pos2);

        this.minX = bounds.minX();
        this.maxX = bounds.maxX();
        this.minY = bounds.minY();
        this.maxY = bounds.maxY();
        this.minZ = bounds.minZ();
        this.maxZ = bounds.maxZ();
    }

    private Area(Location pos1, Location pos2, String worldName, String flatName, String locationString,
            Bounds bounds) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.worldName = worldName;
        this.flatName = flatName;
        this.locationString = locationString;
        this.minX = bounds.minX();
        this.maxX = bounds.maxX();
        this.minY = bounds.minY();
        this.maxY = bounds.maxY();
        this.minZ = bounds.minZ();
        this.maxZ = bounds.maxZ();
    }

    /**
     * Constructs a new {@code Area} directly from raw coordinate data and a world name,
     * without requiring the world to be currently loaded.
     * <p>
     * This is used when loading persisted area data from the database. The {@link Location}
     * objects for {@code pos1} and {@code pos2} will have a {@code null} world reference if
     * the specified world is not currently loaded; operations that need an active world
     * (such as {@link #getAllOuterBlocks()}) already guard against a {@code null} world and
     * will return empty/safe results until the world becomes available.
     *
     * @param worldName The name of the world this area belongs to. Must not be null.
     * @param bounds    The area's normalized minimum and maximum coordinates.
     * @param flatName  The name of the flat this area belongs to. Must not be null.
     * @return A new {@code Area} that holds the world name and coordinates in memory even
     * when the world is not currently loaded.
     */
    public static Area fromRawData(@NotNull String worldName, @NotNull Bounds bounds, @NotNull String flatName) {
        World world = Bukkit.getWorld(worldName);
        Location pos1 = new Location(world, bounds.minX(), bounds.minY(), bounds.minZ());
        Location pos2 = new Location(world, bounds.maxX(), bounds.maxY(), bounds.maxZ());
        return new Area(pos1, pos2, worldName, flatName,
                bounds.toLocationString(worldName), bounds);
    }

    /**
     * Creates a new {@link Area} instance by parsing location data from a string representation.
     *
     * @param locationString A non-null string representing two {@link Location} objects.
     *                       The format must be {@code worldName:x1,y1,z1;x2,y2,z2}.
     * @param flatName       A non-null string representing the name of the flat.
     * @return A new {@link Area} instance created using the parsed locations and the provided flat name.
     * @throws IllegalArgumentException if the {@code locationString} is malformed or invalid.
     */
    public static Area fromString(@NotNull String locationString, @NotNull String flatName) {
        Location[] locations = LocationConverter.getLocationsFromString(locationString);
        return new Area(locations[0], locations[1], flatName);
    }

    /**
     * Creates an {@link Area} instance from a given {@link Selection} object and a flat name.
     *
     * @param selection The {@link Selection} defining the positions of the area. Must not be null.
     * @param flatName  The name associated with the flat. Must not be null.
     * @return A new {@link Area} created using the positions defined in the {@link Selection} and the specified flat name.
     */
    public static Area fromSelection(@NotNull Selection selection, @NotNull String flatName) {
        return new Area(selection.getPos1(), selection.getPos2(), flatName);
    }

    /**
     * Checks whether a location lies inside this area or within the given horizontal range of it.
     *
     * @param location The location to check.
     * @param range    The maximum horizontal distance from the area's bounds.
     * @return {@code true} if the location is inside the area or close enough on the X/Z plane.
     */
    public boolean isWithinDistance(@NotNull Location location, double range) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
            return false;
        }

        double clampedX = Math.clamp(location.getX(), minX, maxX);
        double clampedZ = Math.clamp(location.getZ(), minZ, maxZ);
        double deltaX = location.getX() - clampedX;
        double deltaZ = location.getZ() - clampedZ;

        return (deltaX * deltaX) + (deltaZ * deltaZ) <= range * range;
    }

    /**
     * Retrieves all outer boundary blocks of the area defined by this {@link Area} object.
     * <p>
     * The method calculates and includes blocks along the edges of the three-dimensional
     * space defined by the corner points {@code pos1} and {@code pos2}.
     *
     * @return A non-null {@link List} of {@link Block} objects representing the outer boundary
     * blocks of the defined area. The list will be empty if the world associated with
     * {@code pos1} is {@code null}.
     */
    public @NotNull List<Block> getAllOuterBlocks() {
        List<Block> blocks = new ArrayList<>();
        World world = pos1.getWorld();
        if (world == null) {
            return blocks;
        }

        addXYPlanes(blocks, world);
        addXZPlanes(blocks, world);
        addYZPlanes(blocks, world);
        return blocks;
    }

    private void addXYPlanes(List<Block> blocks, World world) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                blocks.add(world.getBlockAt(x, y, minZ));
                if (maxZ > minZ) {
                    blocks.add(world.getBlockAt(x, y, maxZ));
                }
            }
        }
    }

    private void addXZPlanes(List<Block> blocks, World world) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                blocks.add(world.getBlockAt(x, minY, z));
                if (maxY > minY) {
                    blocks.add(world.getBlockAt(x, maxY, z));
                }
            }
        }
    }

    private void addYZPlanes(List<Block> blocks, World world) {
        for (int y = minY + 1; y < maxY; y++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                blocks.add(world.getBlockAt(minX, y, z));
                if (maxX > minX) {
                    blocks.add(world.getBlockAt(maxX, y, z));
                }
            }
        }
    }

    public record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {

        public static Bounds fromLocations(@NotNull Location pos1, @NotNull Location pos2) {
            return new Bounds(
                    Math.min(pos1.getBlockX(), pos2.getBlockX()),
                    Math.max(pos1.getBlockX(), pos2.getBlockX()),
                    Math.min(pos1.getBlockY(), pos2.getBlockY()),
                    Math.max(pos1.getBlockY(), pos2.getBlockY()),
                    Math.min(pos1.getBlockZ(), pos2.getBlockZ()),
                    Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
        }

        private String toLocationString(String worldName) {
            return worldName + ":" + minX + "," + minY + "," + minZ + ";" + maxX + "," + maxY + "," + maxZ;
        }
    }

}
