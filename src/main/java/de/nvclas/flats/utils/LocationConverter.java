package de.nvclas.flats.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class LocationConverter {

    public static @NotNull String getStringFromLocations(@NotNull Location pos1, @NotNull Location pos2) {
        String pos1String = pos1.getBlockX() + "," + pos1.getBlockY() + "," + pos1.getBlockZ();
        String pos2String = pos2.getBlockX() + "," + pos2.getBlockY() + "," + pos2.getBlockZ();
        return pos1.getWorld().getName() + ":" + pos1String + ";" + pos2String;
    }
    
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
