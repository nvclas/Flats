package de.nvclas.flats.config;

import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlatsConfig extends Config {

    private static final String FLATS_SECTION = "flats";

    public FlatsConfig(String fileName) {
        super(fileName);
    }

    public void saveFlats(Map<String, Flat> flats) {
        getConfigFile().set(FLATS_SECTION, null);
        flats.forEach(this::saveFlat);
        saveConfig();
    }

    public Map<String, Flat> loadFlats() {
        ConfigurationSection flatsSection = getConfigFile().getConfigurationSection(FLATS_SECTION);
        if (flatsSection == null) {
            return new HashMap<>();
        }
        
        return flatsSection
                .getKeys(false)
                .stream()
                .map(this::loadFlat)
                .collect(HashMap::new, (map, flat) -> map.put(flat.getName(), flat), HashMap::putAll);
    }

    private void saveFlat(String flatName, Flat flat) {
        getConfigFile().set(Paths.getOwnerPath(flatName), flat.getOwner() == null ? null : flat.getOwner()
                .getUniqueId()
                .toString());

        getConfigFile().set(Paths.getAreasPath(flatName), flat.getAreas()
                .stream()
                .map(Area::getLocationString)
                .toList());
    }

    private Flat loadFlat(String flatName) {
        String ownerUuid = getConfigFile().getString(Paths.getOwnerPath(flatName));
        List<String> areaStrings = getConfigFile().getStringList(Paths.getAreasPath(flatName));

        OfflinePlayer owner = (ownerUuid != null && !ownerUuid.isEmpty()) ? Bukkit.getOfflinePlayer(UUID.fromString(ownerUuid)) : null;

        List<Area> areas = areaStrings.stream()
                .map(areaString -> Area.fromString(areaString, flatName))
                .toList();

        return new Flat(flatName, areas, owner);
    }
}