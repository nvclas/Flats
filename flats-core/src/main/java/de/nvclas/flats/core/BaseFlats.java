package de.nvclas.flats.core;

import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.commands.MainCommand;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.listeners.FlatEnteredOrLeftListener;
import de.nvclas.flats.core.listeners.PlayerChangedWorldListener;
import de.nvclas.flats.core.listeners.PlayerMoveListener;
import de.nvclas.flats.core.listeners.StickInteractListener;
import de.nvclas.flats.core.schedulers.CommandDelayScheduler;
import de.nvclas.flats.core.storage.StorageAdapter;
import de.nvclas.flats.core.util.I18n;
import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

@Getter
public abstract class BaseFlats extends JavaPlugin {

    private ConfigAdapter configAdapter;
    private StorageAdapter storageAdapter;
    private FlatsCache flatsCache;

    public abstract @NotNull String getPrefix();

    public abstract @NotNull String getMainCommandName();

    public abstract @NotNull String getPluginName();

    public abstract @NotNull String getPermissionPrefix();

    protected abstract @NotNull ConfigAdapter createConfigAdapter();

    protected abstract @NotNull StorageAdapter createStorageAdapter();

    protected abstract void registerProtection();

    @Override
    public void onEnable() {
        // Config Adaption
        this.configAdapter = createConfigAdapter();

        // Translations
        I18n.initialize(this);
        I18n.loadTranslations(this.configAdapter.getLanguage());

        // Dynamic Storage Initialization
        this.storageAdapter = createStorageAdapter();

        // Cache
        this.flatsCache = new FlatsCache(this.storageAdapter);

        // Commands & Listeners
        registerMainCommand();
        registerCommonListeners();
        registerProtection();

        getLogger().log(Level.INFO, () -> getPluginName() + " initialized successfully");
    }

    @Override
    public void onDisable() {
        CommandDelayScheduler.stopAll();

        if (this.flatsCache != null) {
            this.flatsCache.shutdown();
        }

        if (this.storageAdapter != null) {
            this.storageAdapter.close();
        }

        getLogger().log(Level.INFO, () -> "Schedulers stopped and storage closed");
    }

    private void registerMainCommand() {
        String commandName = getMainCommandName();
        PluginCommand command = getCommand(commandName);

        if (command != null) {
            MainCommand executor = new MainCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().log(Level.SEVERE, () -> "Command '" + commandName + "' is missing in plugin.yml!");
        }
    }

    private void registerCommonListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new StickInteractListener(this), this);
        pm.registerEvents(new PlayerChangedWorldListener(), this);
        pm.registerEvents(new PlayerMoveListener(this), this);
        pm.registerEvents(new FlatEnteredOrLeftListener(this), this);
    }

}
