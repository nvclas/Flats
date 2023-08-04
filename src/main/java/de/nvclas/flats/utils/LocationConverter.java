package de.nvclas.flats.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationConverter {

    public static String getStringFromSelection(Selection selection) {
        String pos1String =
            selection.getPos1().getBlockX() + "," + selection.getPos1().getBlockY() + ","
                + selection.getPos1().getBlockZ();
        String pos2String =
            selection.getPos2().getBlockX() + "," + selection.getPos2().getBlockY() + ","
                + selection.getPos2().getBlockZ();
        return selection.getPos1().getWorld().getName() + ":" + pos1String + ";" + pos2String;
    }

    public static Selection getSelectionFromString(String locationString) {
        //format "w:x1,y1,z1;x2,y2,z2"
        World world = Bukkit.getWorld(locationString.substring(0, locationString.indexOf(":")));
        locationString = locationString.substring(locationString.indexOf(":") + 1);
        String[] locationParts = locationString.split(";");
        return new Selection(getLocationFromString(world, locationParts[0]),
            getLocationFromString(world, locationParts[1]));
    }

    private static Location getLocationFromString(World w, String s) {
        String[] parts = s.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new Location(w, x, y, z);
    }

}
