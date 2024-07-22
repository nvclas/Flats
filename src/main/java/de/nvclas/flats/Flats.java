package de.nvclas.flats;

import de.nvclas.flats.commands.FlatsCommand;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.listener.PlayerChangedWorldListener;
import de.nvclas.flats.listener.StickInteractListener;
import de.nvclas.flats.utils.I18n;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Flats extends JavaPlugin {

    public static final String PREFIX = "§7[§6Flats§7] §r";

    private FlatsConfig flatsConfig;
    private SettingsConfig settingsConfig;

    @Override
    public void onEnable() {
        flatsConfig = new FlatsConfig(this,"flats.yml");
        settingsConfig = new SettingsConfig(this, "settings.yml");
        
        getCommand("flat").setExecutor(new FlatsCommand(this));

        getServer().getPluginManager().registerEvents(new StickInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerChangedWorldListener(), this);

        I18n.initialize(this);
        I18n.loadTranslations(settingsConfig.getLanguage());
    }

    public static boolean hasNoPermission(Player p, String permission) {
        if (!p.hasPermission(permission)) {
            p.sendMessage(PREFIX + I18n.translate("messages.no_permission"));
            return true;
        }
        return false;
    }

}
