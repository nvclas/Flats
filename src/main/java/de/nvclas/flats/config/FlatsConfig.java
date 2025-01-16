package de.nvclas.flats.config;

import de.nvclas.flats.selection.Selection;
import de.nvclas.flats.utils.LocationConverter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class FlatsConfig extends Config {

    public FlatsConfig(String fileName) {
        super(fileName);
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

    public void setOwner(String flatName, OfflinePlayer owner) {
        if (owner == null) {
            configFile.set(getOwnerPath(flatName), null);
        } else {
            configFile.set(getOwnerPath(flatName), owner.getUniqueId().toString());
        }
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

    private @NotNull String getAreaPath(String flatName) {
        return flatName + ".areas";
    }

    private @NotNull String getOwnerPath(String flatName) {
        return flatName + ".owner";
    }
    
}
