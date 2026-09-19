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

    public static final String ADMIN = "admin";
    public static final String EDIT_FLATS = "edit";
    public static final String CLAIM_FLATS = "claim";
    public static final String SHOW_FLATS = "show";
    public static final String LIST_FLATS = "list";
    public static final String INFO_FLATS = "info";
    public static final String TRUST_PLAYERS = "trust";
    public static final String SKIP_COMMAND_DELAY = "skip_command_delay";

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
     * Builds a full permission node from the plugin-specific permission prefix and suffix.
     *
     * @param node   the permission suffix
     * @param plugin the active plugin instance providing the permission prefix
     * @return the full permission node
     */
    public static @NotNull String getPermission(@NotNull BaseFlats plugin, @NotNull String node) {
        return plugin.getPermissionPrefix() + "." + node;
    }

    /**
     * Checks if the specified player has administrative permissions.
     *
     * @param plugin The active plugin instance providing the permission prefix.
     * @param player The {@link Player} whose permissions are being checked.
     * @return {@code true} if the player has administrative permissions, {@code false} otherwise.
     */
    public static boolean hasAdminPermission(@NotNull BaseFlats plugin, @NotNull Player player) {
        return player.hasPermission(getPermission(plugin, ADMIN));
    }

    /**
     * Determines whether the specified player has permission to edit flats based on the current
     * configuration settings.
     *
     * @param plugin        The active plugin instance providing the permission prefix.
     * @param player        The {@link Player} whose permissions are being checked.
     * @param configAdapter The {@link ConfigAdapter} containing the configuration for permission settings.
     * @return {@code true} if the player has the required permission to edit flats; {@code false} otherwise.
     */
    public static boolean canEditFlats(@NotNull BaseFlats plugin, @NotNull Player player,
            @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(getPermission(plugin, EDIT_FLATS));
        } else {
            return hasAdminPermission(plugin, player);
        }
    }

    /**
     * Determines whether the specified player is allowed to claim flats based on the given configuration.
     *
     * @param plugin        The active plugin instance providing the permission prefix.
     * @param player        The player whose permissions are being checked.
     * @param configAdapter The configuration adapter that provides relevant settings.
     * @return {@code true} if the player is allowed to claim flats, {@code false} otherwise.
     */
    public static boolean canClaimFlats(@NotNull BaseFlats plugin, @NotNull Player player,
            @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(getPermission(plugin, CLAIM_FLATS));
        } else {
            return true;
        }
    }

    /**
     * Checks whether the player is allowed to view flats based on the configured settings and permissions.
     *
     * @param plugin        The active plugin instance providing the permission prefix.
     * @param player        The {@link Player} whose permissions are being checked.
     * @param configAdapter The {@link ConfigAdapter} instance providing the configuration settings.
     * @return {@code true} if the player can view flats; {@code false} otherwise.
     */
    public static boolean canShowFlats(@NotNull BaseFlats plugin, @NotNull Player player,
            @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(getPermission(plugin, SHOW_FLATS));
        } else {
            return true;
        }
    }

    /**
     * Checks whether a player has the necessary permissions to list flats.
     *
     * @param plugin        The active plugin instance providing the permission prefix.
     * @param player        The player whose permissions are being checked.
     * @param configAdapter The configuration containing permission settings.
     * @return {@code true} if the player has the required permission to list flats; {@code false} otherwise.
     */
    public static boolean canListFlats(@NotNull BaseFlats plugin, @NotNull Player player,
            @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(getPermission(plugin, LIST_FLATS));
        } else {
            return hasAdminPermission(plugin, player);
        }
    }

    /**
     * Determines if the player is allowed to view information about flats based on permissions
     * and the configuration settings.
     *
     * @param plugin        The active plugin instance providing the permission prefix.
     * @param player        The {@link Player} whose permissions are being checked.
     * @param configAdapter The {@link ConfigAdapter} object that contains permission settings.
     * @return {@code true} if the player is allowed to view flat information; {@code false} otherwise.
     */
    public static boolean canInfoFlats(@NotNull BaseFlats plugin, @NotNull Player player,
            @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(getPermission(plugin, INFO_FLATS));
        } else {
            return true;
        }
    }

    /**
     * Determines whether the specified player can be trusted based on the provided settings configuration.
     *
     * @param plugin        The active plugin instance providing the permission prefix.
     * @param player        The {@link Player} whose trustworthiness is being checked.
     * @param configAdapter The {@link ConfigAdapter} instance containing permission-related settings.
     * @return {@code true} if the player can be trusted; {@code false} otherwise.
     */
    public static boolean canTrustPlayers(@NotNull BaseFlats plugin, @NotNull Player player,
            @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(getPermission(plugin, TRUST_PLAYERS));
        } else {
            return true;
        }
    }

    /**
     * Determines whether the player can skip the command delay based on their permissions and the configuration settings.
     *
     * @param plugin        The active plugin instance providing the permission prefix.
     * @param player        The {@link Player} whose permissions are being checked.
     * @param configAdapter The {@link ConfigAdapter} instance containing the configuration settings.
     * @return {@code true} if the player can skip the command delay, {@code false} otherwise.
     */
    public static boolean canSkipCommandDelay(@NotNull BaseFlats plugin, @NotNull Player player,
            @NotNull ConfigAdapter configAdapter) {
        if (configAdapter.isAdvancedPermissionsEnabled()) {
            return player.hasPermission(getPermission(plugin, SKIP_COMMAND_DELAY));
        } else {
            return hasAdminPermission(plugin, player);
        }
    }

    /**
     * Checks if a player has no permissions related to flat management.
     *
     * @param plugin        The active plugin instance providing the permission prefix.
     * @param player        The player whose permissions are being checked.
     * @param configAdapter The settings configuration governing the permission checks.
     * @return {@code true} if the player has none of the relevant permissions; {@code false} otherwise.
     */
    public static boolean hasZeroPermissions(@NotNull BaseFlats plugin, @NotNull Player player,
            @NotNull ConfigAdapter configAdapter) {
        return !canEditFlats(plugin, player, configAdapter) && !canClaimFlats(plugin, player, configAdapter) &&
                !canShowFlats(plugin, player, configAdapter) && !canListFlats(plugin, player, configAdapter) &&
                !canInfoFlats(plugin, player, configAdapter) && !canTrustPlayers(plugin, player, configAdapter) &&
                !canSkipCommandDelay(plugin, player, configAdapter);
    }
}
