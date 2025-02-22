package de.nvclas.flats.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for converting {@link Location} objects to and from their string representation.
 */
@UtilityClass
public class LocationConverter {

    /**
     * Converts two {@link Location} objects into a string representation.
     * The resulting string includes the world name and the coordinates of both locations
     * in the format {@code worldName:x1,y1,z1;x2,y2,z2}.
     *
     * @param pos1 The first {@link Location}. Must not be null and must reference a valid world.
     * @param pos2 The second {@link Location}. Must not be null.
     * @return A non-null {@code String} representing the two locations in the format
     * {@code worldName:x1,y1,z1;x2,y2,z2}.
     * @throws IllegalArgumentException If the first location's world reference is null.
     */
    public static @NotNull String getStringFromLocations(@NotNull Location pos1, @NotNull Location pos2) throws IllegalArgumentException {
        if (pos1.getWorld() == null) {
            throw new IllegalArgumentException("First position has no world reference");
        }
        String pos1String = pos1.getBlockX() + "," + pos1.getBlockY() + "," + pos1.getBlockZ();
        String pos2String = pos2.getBlockX() + "," + pos2.getBlockY() + "," + pos2.getBlockZ();
        return pos1.getWorld().getName() + ":" + pos1String + ";" + pos2String;
    }

    /**
     * Parses a string representation of two {@link Location} objects and returns them as an array.
     * The input string should follow the format {@code worldName:x1,y1,z1;x2,y2,z2}.
     *
     * @param locationString A non-null {@code String} representing two locations. The format must include the
     *                       world name and coordinates as described.
     * @return A non-null array containing two {@link Location} objects. The first position corresponds to the
     * first location in the string, and the second to the second location.
     * @throws IllegalArgumentException If the input string is invalid, the coordinates cannot be parsed,
     *                                  or the specified world doesn't exist.
     */
    public static @NotNull Location[] getLocationsFromString(@NotNull String locationString) {
        String[] parts = locationString.split("[,:;]");
        String world = parts[0];
        Location pos1 = createLocation(world, parts[1], parts[2], parts[3]);
        Location pos2 = createLocation(world, parts[4], parts[5], parts[6]);

        return new Location[]{pos1, pos2};
    }

    private static Location createLocation(String worldName, String x, String y, String z) throws IllegalArgumentException {
        if (Bukkit.getWorld(worldName) == null) {
            throw new IllegalArgumentException("World '" + worldName + "' does not exist");
        }
        try {
            return new Location(Bukkit.getWorld(worldName),
                    Integer.parseInt(x),
                    Integer.parseInt(y),
                    Integer.parseInt(z));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinates in location string");
        }
    }
}
