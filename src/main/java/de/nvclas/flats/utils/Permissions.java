package de.nvclas.flats.utils;

import de.nvclas.flats.Flats;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for managing permissions within the application.
 * Contains methods and constants to assist with handling player permissions.
 */
@UtilityClass
public class Permissions {

    public static final String ADMIN = "flats.admin";

    /**
     * Checks if the specified player does not have the given permission.
     * If the player lacks the permission, a no-permission message is sent to the player.
     *
     * @param player     The {@link Player} whose permissions are to be checked. Must not be null.
     * @param permission The permission string to check for. If the player lacks this permission, a message is sent.
     * @return {@code true} if the player does not have the specified permission, {@code false} otherwise.
     */
    public static boolean hasNoPermission(@NotNull Player player, String permission) {
        if (!player.hasPermission(permission)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.no_permission"));
            return true;
        }
        return false;
    }
}
