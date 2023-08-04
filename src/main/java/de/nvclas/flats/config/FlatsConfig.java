package de.nvclas.flats.config;

import de.nvclas.flats.utils.LocationConverter;
import de.nvclas.flats.utils.Selection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class FlatsConfig extends Config {

    public FlatsConfig(String fileName) {
        super(fileName);
    }

    public void addSelection(String flatName, Selection selection) {
        List<String> flats = getAreas(flatName);
        flats.add(LocationConverter.getStringFromSelection(selection));
        getConfig().set(getAreaPath(flatName), flats);
        saveConfig();
    }

    public void setOwner(String flatName, Player owner) {
        if (owner == null) {
            config.set(getOwnerPath(flatName), null);
            return;
        }
        config.set(getOwnerPath(flatName), owner.getUniqueId().toString());
        saveConfig();
    }

    public OfflinePlayer getOwner(String flatName) {
        String ownerUUID = config.getString(getOwnerPath(flatName));
        if (ownerUUID == null) {
            return null;
        }
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

}
