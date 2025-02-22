package de.nvclas.flats.config;

import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

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

    public FlatsConfig(String fileName, JavaPlugin plugin) {
        super(fileName, plugin);
    }

    /**
     * Saves a collection of flats to the configuration file by clearing the existing flats data
     * and persisting each flat.
     * <p>
     * This method removes all currently stored flat data in the configuration file and then adds
     * the provided flats to it. The updated configuration is saved to disk afterward.
     *
     * @param flats A map containing the flats to be saved, where the key is the flat name,
     *              and the value is the {@link Flat} instance representing the flat. Must not be null.
     */
    public void saveFlats(Map<String, Flat> flats) {
        getConfigFile().set(Paths.FLATS, null);
        flats.forEach(this::saveFlat);
        saveConfig();
    }

    /**
     * Loads all defined flats from the configuration file.
     * <p>
     * This method retrieves the flat definitions stored under {@link Paths#FLATS} in the configuration file,
     * processes them into {@link Flat} objects, and returns a map associating each flat's name with its corresponding
     * {@link Flat} instance. If no flats are defined or the configuration section is absent, an empty map is returned.
     *
     * @return A map containing the loaded flats, where the keys are flat names and the values are their respective
     * {@link Flat} objects. Returns an empty map if no flats are defined or if the configuration section is null.
     */
    public Map<String, Flat> loadFlats() {
        ConfigurationSection flatsSection = getConfigFile().getConfigurationSection(Paths.FLATS);
        if (flatsSection == null) {
            return new HashMap<>();
        }

        return flatsSection.getKeys(false)
                .stream()
                .map(this::loadFlat)
                .filter(Objects::nonNull)
                .collect(HashMap::new, (map, flat) -> map.put(flat.getName(), flat), HashMap::putAll);
    }

    private void saveFlat(String flatName, Flat flat) {
        getConfigFile().set(Paths.getOwnerPath(flatName),
                flat.getOwner() == null ? null : flat.getOwner().getUniqueId().toString());

        getConfigFile().set(Paths.getAreasPath(flatName),
                flat.getAreas().stream().map(Area::getLocationString).toList());
    }

    private @Nullable Flat loadFlat(String flatName) {
        String ownerUuid = getConfigFile().getString(Paths.getOwnerPath(flatName));
        List<String> locationStrings = getConfigFile().getStringList(Paths.getAreasPath(flatName));

        OfflinePlayer owner = (ownerUuid != null && !ownerUuid.isEmpty()) ? Bukkit.getOfflinePlayer(UUID.fromString(
                ownerUuid)) : null;

        List<Area> areas = locationStrings.stream().map(locationString -> {
            try {
                return Area.fromString(locationString, flatName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger()
                        .log(Level.WARNING,
                                () -> "Flat '" + flatName + "' has an invalid area string '" + locationString + "' and will not be loaded.");
                //noinspection ReturnOfNull
                return null;
            }
        }).filter(Objects::nonNull).toList();

        if (areas.isEmpty()) {
            plugin.getLogger()
                    .log(Level.WARNING, () -> "Flat '" + flatName + "' has no valid areas and will not be loaded.");
            return null;
        }

        if (areas.size() < locationStrings.size()) {
            plugin.getLogger()
                    .log(Level.WARNING,
                            () -> "!! ANY INVALID AREAS WILL BE REMOVED ON NEXT SAVE, PLEASE BACKUP NOW IF THEY ARE STILL NEEDED !!");
        }

        return new Flat(flatName, areas, owner);
    }
}