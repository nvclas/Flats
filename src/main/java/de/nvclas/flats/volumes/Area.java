package de.nvclas.flats.volumes;

import de.nvclas.flats.util.LocationConverter;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Creates a new {@link Area} instance using raw data including world name, bounds, and flat name.
     * <p>
     * This method initializes the area based on the corners defined by the {@link Bounds} object
     * and associates it with the specified flat name and world.
     *
     * @param worldName The name of the world in which the area resides. Must not be null.
     * @param bounds    The {@link Bounds} object defining the minimum and maximum corner coordinates of the area. Must not be null.
     * @param flatName  The name of the flat associated with this area. Must not be null.
     * @return A new {@link Area} object initialized with the specified data.
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
     * Returns the world this area is located in.
     *
     * @return the {@link World} this area belongs to, or {@code null} if that world
     * is not currently loaded
     */
    public @Nullable World getWorld() {
        return pos1.getWorld();
    }

    /**
     * Computes the 12 edges of this area's bounding box, in world space (i.e. at block
     * boundaries, one block "outside" the max coordinates, so the frame wraps the full
     * volume rather than cutting through the outer blocks).
     *
     * @return a list of the 12 {@link Edge}s of the bounding box, or an empty list if this
     * area's world is not currently loaded.
     */
    public @NotNull List<Edge> getEdges() {
        World world = getWorld();
        if (world == null) {
            return List.of();
        }

        double x1 = minX, y1 = minY, z1 = minZ;
        double x2 = maxX + 1.0, y2 = maxY + 1.0, z2 = maxZ + 1.0;

        Location[] corners = {
                new Location(world, x1, y1, z1),
                new Location(world, x2, y1, z1),
                new Location(world, x1, y2, z1),
                new Location(world, x1, y1, z2),
                new Location(world, x2, y2, z1),
                new Location(world, x2, y1, z2),
                new Location(world, x1, y2, z2),
                new Location(world, x2, y2, z2)
        };

        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < corners.length; i++) {
            for (int j = i + 1; j < corners.length; j++) {
                if (isEdge(corners[i], corners[j])) {
                    edges.add(new Edge(corners[i], corners[j]));
                }
            }
        }
        return edges;
    }

    private static boolean isEdge(@NotNull Location a, @NotNull Location b) {
        int sameCoords = 0;
        if (a.getX() == b.getX())
            sameCoords++;
        if (a.getY() == b.getY())
            sameCoords++;
        if (a.getZ() == b.getZ())
            sameCoords++;
        return sameCoords == 2;
    }

    /**
     * Represents an edge between two {@link Location} points.
     * <p>
     * An {@code Edge} defines a straight connection from a start location to an end location
     * within a three-dimensional space.
     *
     * @param start The starting {@link Location} of the edge. Must not be null.
     * @param end   The ending {@link Location} of the edge. Must not be null.
     */
    public record Edge(Location start, Location end) {
    }

    /**
     * Represents a three-dimensional bounding box defined by minimum and maximum
     * coordinates along the X, Y, and Z axes.
     * <p>
     * The bounds encapsulate a cuboid shape and are immutable.
     * This can be used for spatial calculations, region definitions, or boundary checks.
     *
     * @param minX The smallest X coordinate within the bounds (inclusive).
     * @param maxX The largest X coordinate within the bounds (inclusive).
     * @param minY The smallest Y coordinate within the bounds (inclusive).
     * @param maxY The largest Y coordinate within the bounds (inclusive).
     * @param minZ The smallest Z coordinate within the bounds (inclusive).
     * @param maxZ The largest Z coordinate within the bounds (inclusive).
     */
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
