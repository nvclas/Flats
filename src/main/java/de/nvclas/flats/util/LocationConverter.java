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
     * Converts two {@link Location} objects into a single string representation.
     * The resulting string includes the world name and the block coordinates of each location.
     *
     * @param pos1 The first {@link Location}, must not be null.
     * @param pos2 The second {@link Location}, must not be null.
     * @return A non-null string representing the two locations in the format
     *         {@code "worldName:x1,y1,z1;x2,y2,z2"}.
     */
    public static @NotNull String getStringFromLocations(@NotNull Location pos1, @NotNull Location pos2) {
        String pos1String = pos1.getBlockX() + "," + pos1.getBlockY() + "," + pos1.getBlockZ();
        String pos2String = pos2.getBlockX() + "," + pos2.getBlockY() + "," + pos2.getBlockZ();
        return pos1.getWorld().getName() + ":" + pos1String + ";" + pos2String;
    }

    /**
     * Converts a string representation of two {@link Location} objects into an array of {@link Location}.
     * The input string must specify a world name followed by the coordinates of two locations
     * in the format {@code worldName:x1,y1,z1;x2,y2,z2}.
     *
     * @param locationString A non-null string containing the world name and the coordinates for two locations.
     *                       The format must be {@code worldName:x1,y1,z1;x2,y2,z2}.
     * @return An array of two {@link Location} objects parsed from the provided string.
     * @throws IllegalArgumentException if the location string is malformed or does not contain valid coordinates.
     */
    public static @NotNull Location[] getLocationsFromString(@NotNull String locationString) {
        String[] parts = locationString.split("[,:;]");
        String world = parts[0];
        Location pos1 = createLocation(world, parts[1], parts[2], parts[3]);
        Location pos2 = createLocation(world, parts[4], parts[5], parts[6]);

        return new Location[]{pos1, pos2};
    }

    private static Location createLocation(String worldName, String x, String y, String z) {
        return new Location(Bukkit.getWorld(worldName), Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
    }
}
