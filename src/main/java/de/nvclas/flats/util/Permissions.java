package de.nvclas.flats.util;

import de.nvclas.flats.Flats;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for managing and checking player permissions.
 */
@UtilityClass
public class Permissions {

    public static final String ADMIN = "flats.admin";

    /**
     * Checks if the specified player lacks the required permission and sends a no-permission message if true.
     *
     * @param player     The player whose permissions are being checked. Must not be null.
     * @param permission The permission string to check against. Could represent an admin or other specific permission.
     * @return {@code true} if the player does not have the required permission, otherwise {@code false}.
     */
    public static boolean hasNoPermission(@NotNull Player player, String permission) {
        if (!player.hasPermission(permission)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.no_permission"));
            return true;
        }
        return false;
    }
}
