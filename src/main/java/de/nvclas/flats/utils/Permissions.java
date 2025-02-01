package de.nvclas.flats.utils;

import de.nvclas.flats.Flats;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Permissions {

    public static final String ADMIN = "flats.admin";

    public static boolean hasNoPermission(@NotNull Player p, String permission) {
        if (!p.hasPermission(permission)) {
            p.sendMessage(Flats.PREFIX + I18n.translate("messages.no_permission"));
            return true;
        }
        return false;
    }
}
