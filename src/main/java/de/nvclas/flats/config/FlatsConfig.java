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

/**
 * The {@code FlatsConfig} class extends {@link Config} and provides methods to manage
 * the configuration data specifically for flats in a file-based system. It handles
 * saving, loading, and processing flat-related configuration data, leveraging
 * the underlying functionality provided by the {@link Config} class.
 * <p>
 * This class utilizes keys and utilities defined in the {@link Paths} class for
 * consistent and structured configuration file interactions.
 */
public class FlatsConfig extends Config {

    public FlatsConfig(String fileName) {
        super(fileName);
    }

    /**
     * Saves the provided flats to the configuration file.
     * <p>
     * This method clears any existing flat data in the configuration under the key
     * defined by {@link Paths#FLATS}, then saves each flat by invoking {@code saveFlat}
     * for each entry in the provided map. Finally, it writes the changes to the configuration
     * file by calling {@link #saveConfig()}.
     *
     * @param flats A map where the keys are the flat names (as {@code String}) and the values
     *              are the corresponding {@link Flat} objects to be saved.
     */
    public void saveFlats(Map<String, Flat> flats) {
        getConfigFile().set(Paths.FLATS, null);
        flats.forEach(this::saveFlat);
        saveConfig();
    }

    /**
     * Loads all flats configuration data from the configuration file into a map.
     * <p>
     * This method retrieves the section of the configuration related to flats using the
     * {@link ConfigurationSection} provided by {@link #getConfigFile()}.
     * For each flat identifier found, it calls {@link #loadFlat(String)} to create and map
     * the corresponding {@code Flat} object.
     * If the relevant configuration section does not exist, an empty map is returned.
     *
     * @return A {@link Map} where the key is the name of the flat and the value is the
     * corresponding {@link Flat} object. If no flats are found, the map will be empty.
     */
    public Map<String, Flat> loadFlats() {
        ConfigurationSection flatsSection = getConfigFile().getConfigurationSection(Paths.FLATS);
        if (flatsSection == null) {
            return new HashMap<>();
        }

        return flatsSection.getKeys(false)
                .stream()
                .map(this::loadFlat)
                .collect(HashMap::new, (map, flat) -> map.put(flat.getName(), flat), HashMap::putAll);
    }

    private void saveFlat(String flatName, Flat flat) {
        getConfigFile().set(Paths.getOwnerPath(flatName),
                flat.getOwner() == null ? null : flat.getOwner().getUniqueId().toString());

        getConfigFile().set(Paths.getAreasPath(flatName),
                flat.getAreas().stream().map(Area::getLocationString).toList());
    }

    private Flat loadFlat(String flatName) {
        String ownerUuid = getConfigFile().getString(Paths.getOwnerPath(flatName));
        List<String> areaStrings = getConfigFile().getStringList(Paths.getAreasPath(flatName));

        OfflinePlayer owner = (ownerUuid != null && !ownerUuid.isEmpty()) ? Bukkit.getOfflinePlayer(UUID.fromString(
                ownerUuid)) : null;

        List<Area> areas = areaStrings.stream().map(areaString -> Area.fromString(areaString, flatName)).toList();

        return new Flat(flatName, areas, owner);
    }
}