package de.nvclas.flats;

import de.nvclas.flats.commands.FlatsCommand;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.listeners.PlayerChangedWorldListener;
import de.nvclas.flats.listeners.StickInteractListener;
import de.nvclas.flats.utils.I18n;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

@Getter
public class Flats extends JavaPlugin {

    public static final String PREFIX = "§7[§6Flats§7] §r";

    private FlatsConfig flatsConfig;
    private SettingsConfig settingsConfig;

    @Override
    public void onEnable() {
        //Configs
        flatsConfig = new FlatsConfig(this, "flats.yml");
        settingsConfig = new SettingsConfig(this, "settings.yml");

        //Commands
        Objects.requireNonNull(getCommand("flats")).setExecutor(new FlatsCommand(this));

        //Listeners
        getServer().getPluginManager().registerEvents(new StickInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerChangedWorldListener(), this);

        //Translations
        I18n.initialize(this);
        I18n.loadTranslations(settingsConfig.getLanguage());
    }

}
