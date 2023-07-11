package de.nvclas.flats.config;

import de.nvclas.flats.utils.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class FlatsConfig extends Config {

    public FlatsConfig(String fileName) {
        super(fileName);
    }

    public void addSelection(String flatName, Selection selection) {
        List<String> flats = getAreas(flatName);
        flats.add(getStringFromSelection(selection));
        getConfig().set(getAreaPath(flatName), flats);
        saveConfig();
    }

    public void setOwner(String flatName, Player owner) {
        config.set(getOwnerPath(flatName), owner.getUniqueId().toString());
        saveConfig();
    }

    public OfflinePlayer getOwner(String flatName) {
        String ownerUUID = config.getString(getOwnerPath(flatName));
        if (ownerUUID == null) return null;
        return Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
    }

    public List<String> getAreas(String flatName) {
        return config.getStringList(getAreaPath(flatName));
    }

    public String getAreaPath(String flatName) {
        return flatName + ".areas";
    }

    public String getOwnerPath(String flatName) {
        return flatName + ".owner";
    }

    private String getStringFromSelection(Selection selection) {
        String pos1String = selection.getPos1().getBlockX() + "," + selection.getPos1().getBlockY() + "," + selection.getPos1().getBlockZ();
        String pos2String = selection.getPos2().getBlockX() + "," + selection.getPos2().getBlockY() + "," + selection.getPos2().getBlockZ();
        return selection.getPos1().getWorld().getName() + ":" + pos1String + ";" + pos2String;
    }

    public Selection getSelectionFromString(String locationString) {
        //format "w:x1,y1,z1;x2,y2,z2"
        World world = Bukkit.getWorld(locationString.substring(0, locationString.indexOf(":")));
        locationString = locationString.substring(locationString.indexOf(":") + 1);
        String[] locationParts = locationString.split(";");
        return new Selection(getLocationFromString(world, locationParts[0]), getLocationFromString(world, locationParts[1]));
    }

    private Location getLocationFromString(World w, String s) {
        String[] parts = s.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new Location(w, x, y, z);
    }

}
