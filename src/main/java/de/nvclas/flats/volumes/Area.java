package de.nvclas.flats.volumes;

import de.nvclas.flats.util.LocationConverter;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
     * Creates a new {@link Area} instance based on a string representation of two {@link Location} objects
     * and a flat name.
     * <p>
     * The method uses the {@link LocationConverter#getLocationsFromString(String)} method to extract two
     * {@link Location} objects from the provided string. These locations define the bounds of the area.
     *
     * @param locationString A non-null string containing the world name and the coordinates of two locations
     *                       in the specified format {@code worldName:x1,y1,z1;x2,y2,z2}.
     * @param flatName       A non-null string representing the name of the flat associated with the area.
     * @return A new {@link Area} instance defined by the parsed {@link Location} objects and the given flat name.
     */
    public static Area fromString(@NotNull String locationString, @NotNull String flatName) {
        Location[] locations = LocationConverter.getLocationsFromString(locationString);
        return new Area(locations[0], locations[1], flatName);
    }

    /**
     * Creates a new {@link Area} instance based on the provided {@link Selection} and flat name.
     * <p>
     * This method uses the two positions from the selection ({@code pos1} and {@code pos2})
     * to define the bounds of the area.
     *
     * @param selection The {@link Selection} object containing the two corner positions. Must not be null.
     * @param flatName  The name of the flat associated with the area. Must not be null.
     * @return A new {@link Area} instance defined by the positions from the selection and the given flat name.
     */
    public static Area fromSelection(@NotNull Selection selection, @NotNull String flatName) {
        return new Area(selection.getPos1(), selection.getPos2(), flatName);
    }

    /**
     * Determines if the provided {@link Location} is within the bounds defined by this area.
     * <p>
     * The bounds are determined by the two corner positions {@code pos1} and {@code pos2}.
     * The method evaluates whether the location's x, y, and z coordinates fall between
     * the minimum and maximum values of the respective coordinates of the two corners.
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
     * Determines if a given {@link Location} is within a specified distance (range)
     * of either of the two opposite corner positions defining the area.
     * <p>
     * The distance is calculated for each axis (x, y, z) individually, and the method ensures
     * that all three axes are within the specified range for either corner {@code pos1} or {@code pos2}.
     *
     * @param location The {@link Location} to check. Must not be null.
     * @param range    The allowed distance (range) from either corner position. Must be a non-negative value.
     * @return {@code true} if the specified {@code location} is within the given range
     * of either {@code pos1} or {@code pos2}; {@code false} otherwise.
     */
    public boolean isWithinDistance(@NotNull Location location, double range) {
        return (Math.abs(location.getX() - pos1.getX()) <= range && Math.abs(
                location.getY() - pos1.getY()) <= range && Math.abs(
                location.getZ() - pos1.getZ()) <= range) || (Math.abs(
                location.getX() - pos2.getX()) <= range && Math.abs(location.getY() - pos2.getY()) <= range && Math.abs(
                location.getZ() - pos2.getZ()) <= range);
    }

    /**
     * Retrieves all the blocks that define the outer boundary of a 3D area formed
     * between two opposite corners, {@code pos1} and {@code pos2}.
     * <p>
     * The outer boundary includes blocks on the minimum and maximum edges for
     * each dimension (x, y, z) within the rectangular prism defined by the two
     * corner positions.
     *
     * @return A list of {@link Block} objects representing the outer boundary
     * of the area.
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
