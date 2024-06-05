package de.nvclas.flats;

import de.nvclas.flats.commands.FlatsCommand;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.listener.StickInteractListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Flats extends JavaPlugin {

    public static final String PREFIX = "§7[§6Flats§7] §r";
    public static final String NO_PERM = PREFIX + "§cBruder, keine Rechte!";

    private static Flats instance;
    private FlatsConfig flatsConfig;

    @Override
    public void onEnable() {
        instance = this;
        flatsConfig = new FlatsConfig("flats.yml");

        getCommand("flat").setExecutor(new FlatsCommand());

        getServer().getPluginManager().registerEvents(new StickInteractListener(), this);
    }

    public static Flats getInstance() {
        return instance;
    }

    public FlatsConfig getFlatsConfig() {
        return flatsConfig;
    }

    public static boolean checkPermission(Player p, String permission) {
        if (!p.hasPermission(permission)) {
            p.sendMessage(NO_PERM);
            return false;
        }
        return true;
    }

}
