package de.nvclas.flats.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for converting {@link Location} objects to and from string representations.
 * Provides methods to serialize and deserialize {@link Location} data, enabling storage or transmission
 * in a compact and readable format.
 */
@UtilityClass
public class LocationConverter {

    /**
     * Converts two {@link Location} objects into a string representation that includes the world name
     * and the coordinates of both positions. The format of the returned string is:
     * {@code worldName:x1,y1,z1;x2,y2,z2}.
     *
     * @param pos1 The first {@link Location}, representing the first position. Must not be null.
     * @param pos2 The second {@link Location}, representing the second position. Must not be null.
     * @return A non-null string containing the world name and the coordinates of both positions
     * in the specified format.
     */
    public static @NotNull String getStringFromLocations(@NotNull Location pos1, @NotNull Location pos2) {
        String pos1String = pos1.getBlockX() + "," + pos1.getBlockY() + "," + pos1.getBlockZ();
        String pos2String = pos2.getBlockX() + "," + pos2.getBlockY() + "," + pos2.getBlockZ();
        return pos1.getWorld().getName() + ":" + pos1String + ";" + pos2String;
    }

    /**
     * Converts a string representation of two {@link Location} objects into an array of {@code Location} objects.
     * The input string is expected to be in the format: {@code worldName:x1,y1,z1;x2,y2,z2},
     * where {@code worldName} is the name of the world, and {@code x1,y1,z1} and {@code x2,y2,z2} represent
     * the coordinates of the two locations.
     *
     * @param locationString A non-null string containing the world name and the coordinates of two locations in
     *                       the specified format.
     * @return A non-null array containing two {@link Location} objects parsed from the input string.
     * The array includes the first location at index 0 and the second location at index 1.
     */
    public static @NotNull Location[] getLocationsFromString(@NotNull String locationString) {
        String[] parts = locationString.split("[,:;]");
        String world = parts[0];
        Location pos1 = createLocation(world, parts[1], parts[2], parts[3]);
        Location pos2 = createLocation(world, parts[4], parts[5], parts[6]);

        return new Location[]{pos1, pos2};
    }

    private static Location createLocation(String worldName, String x, String y, String z) {
        return new Location(
                Bukkit.getWorld(worldName),
                Integer.parseInt(x),
                Integer.parseInt(y),
                Integer.parseInt(z)
        );
    }
}
