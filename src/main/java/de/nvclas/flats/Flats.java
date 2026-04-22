package de.nvclas.flats;

import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.FlatsCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.listeners.FlatEnteredOrLeftListener;
import de.nvclas.flats.listeners.PlayerChangedWorldListener;
import de.nvclas.flats.listeners.PlayerMoveListener;
import de.nvclas.flats.listeners.StickInteractListener;
import de.nvclas.flats.listeners.protection.BlockBreakListener;
import de.nvclas.flats.listeners.protection.BlockExplodeListener;
import de.nvclas.flats.listeners.protection.BlockPlaceListener;
import de.nvclas.flats.listeners.protection.EntityChangeBlockListener;
import de.nvclas.flats.listeners.protection.EntityDamageByEntityListener;
import de.nvclas.flats.listeners.protection.EntityExplodeListener;
import de.nvclas.flats.listeners.protection.HangingBreakByEntityListener;
import de.nvclas.flats.listeners.protection.PlayerInteractListener;
import de.nvclas.flats.migration.MigrationManager;
import de.nvclas.flats.schedulers.CommandDelayScheduler;
import de.nvclas.flats.storage.FlatsStorage;
import de.nvclas.flats.util.I18n;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Main plugin class for the Flats plugin.
 * <p>
 * This plugin allows players to create, manage, and protect designated areas called "flats"
 * within a Minecraft server. It provides functionality for defining spatial boundaries,
 * managing ownership and permissions, and enforcing protection rules within these areas.
 * <p>
 * The plugin handles initialization of configurations, caches, commands, listeners, and
 * schedulers necessary for flat management. It also manages the lifecycle of these components
 * during server startup and shutdown.
 */
@Getter
public class Flats extends JavaPlugin {

    public static final String PREFIX = "§7[§6Flats§7] §r";

    private FlatsStorage flatsStorage;
    private SettingsConfig settingsConfig;
    private FlatsCache flatsCache;

    /**
     * Initializes the plugin when it is enabled by the server.
     * <p>
     * This method performs the following initialization steps:
     * <ol>
     *   <li>Loads configuration files</li>
     *   <li>Sets up internationalization</li>
     *   <li>Initializes the SQLite storage</li>
     *   <li>Migrates data from YAML if necessary</li>
     *   <li>Initializes the flats cache</li>
     *   <li>Registers commands</li>
     *   <li>Registers event listeners</li>
     * </ol>
     */
    @Override
    public void onEnable() {
        //Configs
        settingsConfig = new SettingsConfig("settings.yml", this);

        //Translations
        I18n.initialize(this);
        I18n.loadTranslations(settingsConfig.getLanguage());

        //Storage
        flatsStorage = new FlatsStorage(this);

        //Migration
        MigrationManager.migrate(this, flatsStorage);

        //Managers
        flatsCache = new FlatsCache(flatsStorage);

        //Commands
        Objects.requireNonNull(getCommand("flats")).setExecutor(new FlatsCommand(this));
        Objects.requireNonNull(getCommand("flats")).setTabCompleter(new FlatsCommand(this));

        //Listeners
        getServer().getPluginManager().registerEvents(new StickInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChangedWorldListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new FlatEnteredOrLeftListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockExplodeListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityChangeBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new HangingBreakByEntityListener(this), this);

        getLogger().log(Level.INFO, () -> "Flats initialized successfully");
    }

    /**
     * Performs cleanup operations when the server disables the plugin.
     * <p>
     * This method ensures that all plugin resources are properly released and
     * data is saved before the plugin is disabled. It performs the following tasks:
     * <ol>
     *   <li>Stops all command delay schedulers</li>
     *   <li>Closes the database connection</li>
     * </ol>
     */
    @Override
    public void onDisable() {
        //Stop schedulers
        CommandDelayScheduler.stopAll();

        //Close storage
        if (flatsStorage != null) {
            flatsStorage.close();
        }

        getLogger().log(Level.INFO, () -> "Schedulers stopped and storage closed");
    }

}
