package de.nvclas.flats.utils;

import de.nvclas.flats.Flats;
import org.bukkit.entity.Player;

public class Permissions {
    
    private Permissions() {
        throw new IllegalStateException("Utility class");
    }
    
    public static final String ADMIN = "flats.admin";

    public static boolean hasNoPermission(Player p, String permission) {
        if (!p.hasPermission(permission)) {
            p.sendMessage(Flats.PREFIX + I18n.translate("messages.no_permission"));
            return true;
        }
        return false;
    }
}
