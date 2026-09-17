package de.nvclas.flats.core.util;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.config.ConfigAdapter;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for handling permissions in the Flats plugin ecosystem.
 */
@UtilityClass
public class Permissions {

    public static final String ADMIN = "flats.admin";
    public static final String EDIT_FLATS = "flats.edit";
    public static final String CLAIM_FLATS = "flats.claim";
    public static final String SHOW_FLATS = "flats.show";
    public static final String LIST_FLATS = "flats.list";
    public static final String INFO_FLATS = "flats.info";
    public static final String TRUST_PLAYERS = "flats.trust";
    public static final String SKIP_COMMAND_DELAY = "flats.skip_command_delay";

    /**
     * Displays a message to the specified player indicating that they do not have the
     * necessary permissions to perform the desired action.
     *
     * @param plugin the active plugin instance to retrieve the dynamic prefix from
     * @param player the player to whom the no-permission message will be sent
     */
    public static void showNoPermissionMessage(@NotNull BaseFlats plugin, @NotNull Player player) {
        player.sendMessage(plugin.getPrefix() + I18n.translate("error.no_permission"));
    }

    /**
     * Checks if the specified player has administrative permissions.
     *
     * @param player The {@link Player} whose permissions are being checked.
     * @return {@code true} if the player has administrative permissions, {@code false} otherwise.
     */
    public static boolean hasAdminPermission(@NotNull Player player) {
        return player.hasPermission(ADMIN);
    }

    /**
     * Determines whether the specified player has permission to edit flats based on the current
     * configuration settings.
     *
     * @param player        The {@link Player} whose permissions are being checked.
     * @param configAdapter The {@link ConfigAdapter} containing the configuration for permission settings.
     * @return {@code true} if the player has the required permission to edit flats; {@code false} otherwise.
     */
    public static boolean canEditFlats(@NotNull Player player, @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(EDIT_FLATS);
        } else {
            return player.hasPermission(ADMIN);
        }
    }

    /**
     * Determines whether the specified player is allowed to claim flats based on the given configuration.
     *
     * @param player        The player whose permissions are being checked.
     * @param configAdapter The configuration adapter that provides relevant settings.
     * @return {@code true} if the player is allowed to claim flats, {@code false} otherwise.
     */
    public static boolean canClaimFlats(@NotNull Player player, @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(CLAIM_FLATS);
        } else {
            return true;
        }
    }

    /**
     * Checks whether the player is allowed to view flats based on the configured settings and permissions.
     *
     * @param player        The {@link Player} whose permissions are being checked.
     * @param configAdapter The {@link ConfigAdapter} instance providing the configuration settings.
     * @return {@code true} if the player can view flats; {@code false} otherwise.
     */
    public static boolean canShowFlats(@NotNull Player player, @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(SHOW_FLATS);
        } else {
            return true;
        }
    }

    /**
     * Checks whether a player has the necessary permissions to list flats.
     *
     * @param player        The player whose permissions are being checked.
     * @param configAdapter The configuration containing permission settings.
     * @return {@code true} if the player has the required permission to list flats; {@code false} otherwise.
     */
    public static boolean canListFlats(@NotNull Player player, @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(LIST_FLATS);
        } else {
            return player.hasPermission(ADMIN);
        }
    }

    /**
     * Determines if the player is allowed to view information about flats based on permissions
     * and the configuration settings.
     *
     * @param player        The {@link Player} whose permissions are being checked.
     * @param configAdapter The {@link ConfigAdapter} object that contains permission settings.
     * @return {@code true} if the player is allowed to view flat information; {@code false} otherwise.
     */
    public static boolean canInfoFlats(@NotNull Player player, @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(INFO_FLATS);
        } else {
            return true;
        }
    }

    /**
     * Determines whether the specified player can be trusted based on the provided settings configuration.
     *
     * @param player        The {@link Player} whose trustworthiness is being checked.
     * @param configAdapter The {@link ConfigAdapter} instance containing permission-related settings.
     * @return {@code true} if the player can be trusted; {@code false} otherwise.
     */
    public static boolean canTrustPlayers(@NotNull Player player, @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(TRUST_PLAYERS);
        } else {
            return true;
        }
    }

    /**
     * Determines whether the player can skip the command delay based on their permissions and the configuration settings.
     *
     * @param player        The {@link Player} whose permissions are being checked.
     * @param configAdapter The {@link ConfigAdapter} instance containing the configuration settings.
     * @return {@code true} if the player can skip the command delay, {@code false} otherwise.
     */
    public static boolean canSkipCommandDelay(@NotNull Player player, @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(SKIP_COMMAND_DELAY);
        } else {
            return player.hasPermission(ADMIN);
        }
    }

    /**
     * Checks if a player has no permissions related to flat management.
     *
     * @param player        The player whose permissions are being checked.
     * @param configAdapter The settings configuration governing the permission checks.
     * @return {@code true} if the player has none of the relevant permissions; {@code false} otherwise.
     */
    public static boolean hasZeroPermissions(@NotNull Player player, @NotNull ConfigAdapter configAdapter) {
        return !canEditFlats(player, configAdapter) && !canClaimFlats(player, configAdapter) &&
                !canShowFlats(player, configAdapter) && !canListFlats(player, configAdapter) &&
                !canInfoFlats(player, configAdapter) && !canTrustPlayers(player, configAdapter) &&
                !canSkipCommandDelay(player, configAdapter);
    }
}
