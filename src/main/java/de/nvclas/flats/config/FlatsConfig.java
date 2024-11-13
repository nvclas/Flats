package de.nvclas.flats.config;

import de.nvclas.flats.Flats;
import de.nvclas.flats.selection.Flat;
import de.nvclas.flats.selection.Selection;
import de.nvclas.flats.utils.LocationConverter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class FlatsConfig extends Config {

    public FlatsConfig(Flats plugin, String fileName) {
        super(plugin, fileName);
    }

    public void addSelection(String flatName, Selection selection) {
        List<String> flats = getAreas(flatName);
        flats.add(LocationConverter.getStringFromSelection(selection));
        getConfigFile().set(getAreaPath(flatName), flats);
        saveConfig();
    }

    public void removeFlat(String flatName) {
        getConfigFile().set(flatName, null);
        saveConfig();
    }

    public void setOwner(String flatName, Player owner) {
        if (owner == null) {
            configFile.set(getOwnerPath(flatName), null);
            return;
        }
        configFile.set(getOwnerPath(flatName), owner.getUniqueId().toString());
        saveConfig();
    }

    public @Nullable OfflinePlayer getOwner(String flatName) {
        String ownerUUID = configFile.getString(getOwnerPath(flatName));
        if (ownerUUID == null) {
            return null;
        }
        return Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
    }

    public @NotNull List<String> getAreas(String flatName) {
        return configFile.getStringList(getAreaPath(flatName));
    }

    public @NotNull String getAreaPath(String flatName) {
        return flatName + ".areas";
    }

    public @NotNull String getOwnerPath(String flatName) {
        return flatName + ".owner";
    }
    
    public @Nullable Flat getFlatByLocation(Location location) {
        for (String flatName : getConfigFile().getKeys(false)) {
            for (String selectionString : getAreas(flatName)) {
                Selection selection = LocationConverter.getSelectionFromString(selectionString);
                if (selection.intersects(location)) {
                    return new Flat(selection, flatName);
                }
            }
        }
        return null;
    }
    
}
