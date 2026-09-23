package de.nvclas.flats.core.migration;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.storage.SqliteStorage;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

@RequiredArgsConstructor
public class CoreMigrationService implements MigrationService {

    private final BaseFlats plugin;

    @Override
    public void migrate() {
        migrateSqliteFilename(List.of("flats.db"));
    }

    private void migrateSqliteFilename(List<String> oldNames) {
        for (String oldName : oldNames) {
            File dbFile = new File(plugin.getDataFolder(), oldName);
            if (!dbFile.exists()) {
                return;
            }
            File dataFile = new File(plugin.getDataFolder(), SqliteStorage.DATABASE_NAME);
            if (dataFile.exists()) {
                return;
            }
            if (!dbFile.renameTo(new File(plugin.getDataFolder(), SqliteStorage.DATABASE_NAME))) {
                plugin.getLogger()
                        .log(Level.SEVERE,
                                () -> "Failed to migrate database, please rename " + oldName + " to "
                                        + SqliteStorage.DATABASE_NAME
                                        + " manually");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }
    }
}
