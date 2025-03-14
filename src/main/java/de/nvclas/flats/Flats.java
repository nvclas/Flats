package de.nvclas.flats;

import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.FlatsCommand;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.listeners.FlatEnteredOrLeftListener;
import de.nvclas.flats.listeners.PlayerChangedWorldListener;
import de.nvclas.flats.listeners.PlayerMoveListener;
import de.nvclas.flats.listeners.StickInteractListener;
import de.nvclas.flats.listeners.protection.EntityDamageListener;
import de.nvclas.flats.listeners.protection.PlayerInteractListener;
import de.nvclas.flats.schedulers.AutoSaveScheduler;
import de.nvclas.flats.schedulers.CommandDelayScheduler;
import de.nvclas.flats.util.I18n;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

@Getter
public class Flats extends JavaPlugin {

    public static final String PREFIX = "§7[§6Flats§7] §r";

    private FlatsConfig flatsConfig;
    private SettingsConfig settingsConfig;
    private FlatsCache flatsCache;
    private AutoSaveScheduler autoSaveScheduler;

    @Override
    public void onEnable() {
        //Configs
        flatsConfig = new FlatsConfig("flats.yml", this);
        settingsConfig = new SettingsConfig("settings.yml", this);

        //Translations
        I18n.initialize(this);
        I18n.loadTranslations(settingsConfig.getLanguage());

        //Managers
        flatsCache = new FlatsCache(this);

        //Flats
        flatsCache.loadAll();

        //Schedulers
        autoSaveScheduler = new AutoSaveScheduler(this);
        autoSaveScheduler.start();

        //Commands
        Objects.requireNonNull(getCommand("flats")).setExecutor(new FlatsCommand(this));
        Objects.requireNonNull(getCommand("flats")).setTabCompleter(new FlatsCommand(this));

        //Listeners
        getServer().getPluginManager().registerEvents(new StickInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerChangedWorldListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new FlatEnteredOrLeftListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);

        getLogger().log(Level.INFO, () -> "Flats initialized successfully");
    }

    @Override
    public void onDisable() {
        //Save flats
        flatsCache.saveAll();

        //Stop schedulers
        autoSaveScheduler.stop();
        CommandDelayScheduler.stopAll();

        getLogger().log(Level.INFO, () -> "All flats saved and schedulers stopped");
    }

}
