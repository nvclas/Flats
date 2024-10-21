package de.nvclas.flats.utils;

import de.nvclas.flats.selection.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocationConverter {

    private LocationConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static @Nullable String getStringFromSelection(@NotNull Selection selection) {
        if (selection.getPos1().getWorld() == null || selection.getPos2().getWorld() == null) {
            return null;
        }
        String pos1String = selection.getPos1().getBlockX() + "," + selection.getPos1().getBlockY() + "," + selection.getPos1().getBlockZ();
        String pos2String = selection.getPos2().getBlockX() + "," + selection.getPos2().getBlockY() + "," + selection.getPos2().getBlockZ();
        return selection.getPos1().getWorld().getName() + ":" + pos1String + ";" + pos2String;
    }

    @Contract("_ -> new")
    public static @NotNull Selection getSelectionFromString(@NotNull String locationString) {
        //format "w:x1,y1,z1;x2,y2,z2"
        World world = Bukkit.getWorld(locationString.substring(0, locationString.indexOf(":")));
        locationString = locationString.substring(locationString.indexOf(":") + 1);
        String[] locationParts = locationString.split(";");
        return new Selection(getLocationFromString(world, locationParts[0]), getLocationFromString(world, locationParts[1]));
    }

    @Contract("_, _ -> new")
    private static @NotNull Location getLocationFromString(World w, @NotNull String s) {
        String[] parts = s.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new Location(w, x, y, z);
    }

}
