package de.nvclas.flats.selection;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.utils.LocationConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
@AllArgsConstructor
public class Flat {
    private static final FlatsConfig flatsConfig = Flats.getInstance().getFlatsConfig();
    
    private String name;
    
    public boolean isOwner(OfflinePlayer player) {
        OfflinePlayer owner = flatsConfig.getOwner(name);
        if(owner == null) return false;
        return owner.getUniqueId().equals(player.getUniqueId());
    }

    public static @Nullable Flat getFlatByLocation(Location location) {
        for (String flatName : flatsConfig.getConfigFile().getKeys(false)) {
            for (String selectionString : flatsConfig.getAreas(flatName)) {
                Selection selection = LocationConverter.getSelectionFromString(selectionString);
                if (selection.intersects(location)) {
                    return new Flat(flatName);
                }
            }
        }
        return null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flat flat = (Flat) o;
        return Objects.equals(name, flat.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
