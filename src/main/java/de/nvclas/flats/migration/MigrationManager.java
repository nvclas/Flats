package de.nvclas.flats.migration;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.Paths;
import de.nvclas.flats.storage.FlatsStorage;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles the migration of flat data from the old YAML-based storage (flats.yml)
 * to the new SQLite-based storage.
 */
@UtilityClass
public class MigrationManager {

    /**
     * Migrates data from flats.yml to the provided Storage if the database is empty
     * and the YAML file exists. After successful migration, the YAML file is renamed.
     *
     * @param plugin       The plugin instance.
     * @param flatsStorage The new storage instance.
     */
    public static void migrate(Flats plugin, FlatsStorage flatsStorage) {
        File flatsFile = new File(plugin.getDataFolder(), "flats.yml");
        if (!flatsFile.exists()) {
            return;
        }

        if (!flatsStorage.isEmpty()) {
            plugin.getLogger().log(Level.INFO, () -> "Database is not empty, skipping migration from flats.yml");
            return;
        }

        plugin.getLogger().log(Level.INFO, () -> "Migrating flats from flats.yml to SQLite...");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(flatsFile);
        ConfigurationSection flatsSection = config.getConfigurationSection(Paths.FLATS);

        if (flatsSection == null) {
            plugin.getLogger().log(Level.INFO, () -> "No flats found in flats.yml to migrate.");
            return;
        } else {
            int count = 0;
            for (String flatName : flatsSection.getKeys(false)) {
                Flat flat = loadFlat(flatName, config, plugin);
                if (flat != null) {
                    flatsStorage.saveFlat(flat);
                    count++;
                }
            }
            int finalCount = count;
            plugin.getLogger().log(Level.INFO, () -> "Migrated " + finalCount + " flats to SQLite.");
        }

        plugin.getLogger().log(Level.INFO, () -> "Renaming flats.yml to flats.yml.bak");
        File backupFile = new File(plugin.getDataFolder(), "flats.yml.bak");
        try {
            Files.deleteIfExists(backupFile.toPath());
        } catch (IOException _) {
            plugin.getLogger()
                    .log(Level.WARNING, () -> "Could not delete existing backup file. Please remove it manually.");
        }

        if (!flatsFile.renameTo(backupFile)) {
            plugin.getLogger()
                    .log(Level.WARNING,
                            () -> "Could not rename flats.yml to flats.yml.bak. Please remove it manually.");
        }
    }

    private static @Nullable Flat loadFlat(String flatName, YamlConfiguration config, Flats plugin) {
        String ownerUuid = config.getString(Paths.getOwnerPath(flatName));
        OfflinePlayer owner = null;
        if (ownerUuid != null && !ownerUuid.isEmpty()) {
            try {
                owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUuid));
            } catch (IllegalArgumentException _) {
                plugin.getLogger()
                        .log(Level.WARNING, () -> "Invalid owner UUID for flat " + flatName + ": " + ownerUuid);
            }
        }

        List<String> locationStrings = config.getStringList(Paths.getAreasPath(flatName));
        List<Area> areas = new ArrayList<>();
        for (String locStr : locationStrings) {
            try {
                areas.add(Area.fromString(locStr, flatName));
            } catch (IllegalArgumentException _) {
                plugin.getLogger().log(Level.WARNING, () -> "Invalid area string for flat " + flatName + ": " + locStr);
            }
        }

        if (areas.isEmpty()) {
            return null;
        }

        List<String> trustedUuids = config.getStringList(Paths.getTrustedPath(flatName));
        List<OfflinePlayer> trusted = new ArrayList<>();
        for (String uuid : trustedUuids) {
            try {
                trusted.add(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
            } catch (IllegalArgumentException _) {
                plugin.getLogger().log(Level.WARNING, () -> "Invalid trusted UUID for flat " + flatName + ": " + uuid);
            }
        }

        return new Flat(flatName, owner, areas, trusted);
    }
}
