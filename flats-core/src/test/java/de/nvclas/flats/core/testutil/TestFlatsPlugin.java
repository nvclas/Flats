package de.nvclas.flats.core.testutil;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.config.SettingsConfig;
import de.nvclas.flats.core.storage.SqliteStorage;
import de.nvclas.flats.core.storage.StorageAdapter;
import org.jetbrains.annotations.NotNull;

public class TestFlatsPlugin extends BaseFlats {

    @Override
    public @NotNull String getPrefix() {
        return "[Flats] ";
    }

    @Override
    public @NotNull String getMainCommandName() {
        return "flats";
    }

    @Override
    public @NotNull String getPermissionPrefix() {
        return "flats";
    }

    @Override
    protected @NotNull ConfigAdapter createConfigAdapter() {
        return new SettingsConfig("settings.yml", this);
    }

    @Override
    protected @NotNull StorageAdapter createStorageAdapter() {
        return new SqliteStorage(this);
    }

    @Override
    protected void registerProtection() {
        // Dummy implementation for tests
    }

    @Override
    public @NotNull String getPluginName() {
        return "FlatsTest";
    }
}
