package de.nvclas.flats;

import de.nvclas.flats.commands.FlatsCommand;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.listeners.FlatEnteredOrLeftListener;
import de.nvclas.flats.listeners.PlayerChangedWorldListener;
import de.nvclas.flats.listeners.PlayerMoveListener;
import de.nvclas.flats.listeners.StickInteractListener;
import de.nvclas.flats.listeners.protection.EntityDamageListener;
import de.nvclas.flats.listeners.protection.PlayerInteractListener;
import de.nvclas.flats.managers.FlatsManager;
import de.nvclas.flats.schedulers.AutoSaveScheduler;
import de.nvclas.flats.schedulers.CommandDelayScheduler;
import de.nvclas.flats.utils.I18n;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

@SuppressWarnings("java:S6548") // Singleton warning
@Getter
public class Flats extends JavaPlugin {

    public static final String PREFIX = "§7[§6Flats§7] §r";
    @Getter
    private static Flats instance;

    private FlatsConfig flatsConfig;
    private SettingsConfig settingsConfig;

    @SuppressWarnings("java:S2696") // Required for Singleton
    @Override
    public void onEnable() {
        instance = this;

        //Configs
        flatsConfig = new FlatsConfig("flats.yml");
        settingsConfig = new SettingsConfig("settings.yml");

        //Managers
        FlatsManager.loadAll();

        //Schedulers
        AutoSaveScheduler.start();

        //Commands
        Objects.requireNonNull(getCommand("flats")).setExecutor(new FlatsCommand());
        Objects.requireNonNull(getCommand("flats")).setTabCompleter(new FlatsCommand());

        //Listeners
        getServer().getPluginManager().registerEvents(new StickInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerChangedWorldListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(), this);
        getServer().getPluginManager().registerEvents(new FlatEnteredOrLeftListener(), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);


        //Translations
        I18n.initialize(this);
        I18n.loadTranslations(settingsConfig.getLanguage());

        getLogger().log(Level.INFO, () -> "Flats initialized successfully");
    }

    @Override
    public void onDisable() {
        //Save config
        FlatsManager.saveAll();

        //Stop schedulers
        AutoSaveScheduler.stop();
        CommandDelayScheduler.stopAll();

        getLogger().log(Level.INFO, () -> "All flats saved and schedulers stopped");
    }
}
