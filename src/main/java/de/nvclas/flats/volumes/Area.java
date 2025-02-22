package de.nvclas.flats.volumes;

import de.nvclas.flats.util.LocationConverter;
import lombok.Getter;
import org.bukkit.Location;
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
    private final String locationString;

    public Area(Location pos1, Location pos2, String flatName) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.flatName = flatName;
        this.locationString = LocationConverter.getStringFromLocations(pos1, pos2);
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
     * Checks whether the specified {@link Location} is within the bounds defined
     * by the two corners {@code pos1} and {@code pos2} of this {@link Area}.
     * <p>
     * The method performs a bounding box check across all dimensions (X, Y, Z).
     *
     * @param location The {@link Location} to check. Must not be null.
     * @return {@code true} if the {@code location} is within the bounds of the area;
     * {@code false} otherwise.
     */
    public boolean isWithinBounds(@NotNull Location location) {
        double minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        double maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        double minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        double maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        double minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        double maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return location.getBlockX() >= minX && location.getBlockX() <= maxX && location.getBlockY() >= minY && location.getBlockY() <= maxY && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    /**
     * Checks whether the given {@link Location} is within a specified distance from
     * any of the two positions defining this {@link Area}.
     * <p>
     * This method evaluates whether the provided {@code location} falls within the
     * given {@code range} from either {@code pos1} or {@code pos2} in three-dimensional space.
     *
     * @param location The {@link Location} to be checked. Must not be null.
     * @param range    The distance threshold to check against.
     * @return {@code true} if the {@code location} is within the specified {@code range}
     * from either {@code pos1} or {@code pos2}; {@code false} otherwise.
     */
    public boolean isWithinDistance(@NotNull Location location, double range) {
        return (Math.abs(location.getX() - pos1.getX()) <= range && Math.abs(location.getY() - pos1.getY()) <= range && Math.abs(
                location.getZ() - pos1.getZ()) <= range) || (Math.abs(location.getX() - pos2.getX()) <= range && Math.abs(
                location.getY() - pos2.getY()) <= range && Math.abs(location.getZ() - pos2.getZ()) <= range);
    }

    /**
     * Retrieves a list of all blocks that form the outer boundary of the current area.
     * <p>
     * The method calculates the outer boundary based on the minimum and maximum coordinates
     * derived from {@code pos1} and {@code pos2}, effectively including all blocks
     * located on the edges of the rectangular cuboid defined by the area.
     *
     * @return A {@link List} of {@link Block} instances representing the outer boundary of the area.
     * The returned list is never null but may be empty if no valid boundaries are defined.
     */
    public @NotNull List<Block> getAllOuterBlocks() {
        List<Block> blocks = new ArrayList<>();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                        blocks.add(pos1.getWorld().getBlockAt(x, y, z));
                    }
                }
            }
        }
        return blocks;
    }

}
