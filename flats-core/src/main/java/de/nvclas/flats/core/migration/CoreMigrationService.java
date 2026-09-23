package de.nvclas.flats.core.migration;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.storage.SqliteStorage;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

@RequiredArgsConstructor
public class CoreMigrationService implements MigrationService {

    private static final List<String> OLD_DB_NAMES = List.of("flats.db");
    private static final String NEW_DB_NAME = SqliteStorage.DATABASE_NAME;

    private final BaseFlats plugin;

    @Override
    public void migrate() {
        for (String oldDbName : OLD_DB_NAMES) {
            File dbFile = new File(plugin.getDataFolder(), oldDbName);
            if (!dbFile.exists()) {
                return;
            }
            File dataFile = new File(plugin.getDataFolder(), NEW_DB_NAME);
            if (dataFile.exists()) {
                return;
            }
            if (!dbFile.renameTo(new File(plugin.getDataFolder(), NEW_DB_NAME))) {
                plugin.getLogger()
                        .log(Level.SEVERE,
                                () -> "Failed to migrate database, please rename " + oldDbName + " to " + NEW_DB_NAME
                                        + " manually");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }
    }
}
