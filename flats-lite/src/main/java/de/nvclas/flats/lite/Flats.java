package de.nvclas.flats.lite;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.config.SettingsConfig;
import de.nvclas.flats.core.listeners.ProtectionListener;
import de.nvclas.flats.core.storage.SqliteStorage;
import de.nvclas.flats.core.storage.StorageAdapter;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

public class Flats extends BaseFlats {
    @Override
    public @NotNull String getPrefix() {
        return "§7[§6Flats§7] §r";
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
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ProtectionListener(this), this);
    }

    @Override
    public @NotNull String getPluginName() {
        return "Flats";
    }
}
