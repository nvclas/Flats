package de.nvclas.flats.core.migration;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.config.ConfigAdapter;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public class CoreMigrationService implements MigrationService {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;

    public CoreMigrationService(BaseFlats plugin) {
        this.plugin = plugin;
        this.configAdapter = plugin.getConfigAdapter();
    }

    @Override
    public void migrate() {
        migrateSqliteFileName(List.of("flats.db", "data.sqlite"));
    }

    private void migrateSqliteFileName(List<String> oldNames) {
        for (String oldName : oldNames) {
            File oldDbFile = new File(plugin.getDataFolder(), oldName);
            File newDbFile = new File(plugin.getDataFolder(), configAdapter.getSqliteFileName());
            if (!oldDbFile.exists() || newDbFile.exists()) {
                continue;
            }
            if (!oldDbFile.renameTo(newDbFile)) {
                plugin.getLogger()
                        .log(Level.SEVERE, () -> "Failed to migrate database, please rename " + oldName + " to "
                                + configAdapter.getSqliteFileName() + " manually");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }
    }
}
