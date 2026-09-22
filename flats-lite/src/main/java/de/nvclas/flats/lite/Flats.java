package de.nvclas.flats.lite;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.config.SettingsConfig;
import de.nvclas.flats.core.listeners.ProtectionListener;
import de.nvclas.flats.core.storage.SqliteStorage;
import de.nvclas.flats.core.storage.StorageAdapter;
import de.nvclas.flats.core.updater.UpdateService;
import de.nvclas.flats.lite.updater.GitHubUpdateService;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

public class Flats extends BaseFlats {

    private static final String GITHUB_URL = "https://api.github.com/repos/nvclas/Flats/releases/latest";

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
    protected @NotNull UpdateService createUpdateService() {
        return new GitHubUpdateService(this, GITHUB_URL);
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
