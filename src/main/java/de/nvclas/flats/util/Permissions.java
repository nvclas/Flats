package de.nvclas.flats.util;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.SettingsConfig;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for handling permissions in the Flats plugin.
 * <p>
 * This class provides constants for various permission nodes used in the plugin and
 * utility methods for assessing player permissions. Its methods consider both basic
 * and advanced permission configurations.
 * <p>
 * All methods in this class are static and can be used to verify if a player has
 * specific permissions related to flat management tasks.
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
     * <p>This method sends a localized error message prefixed with the Flats plugin prefix
     * to ensure a consistent user interface experience.
     *
     * @param player the player to whom the no-permission message will be sent; must not be null
     */
    public static void showNoPermissionMessage(@NotNull Player player) {
        player.sendMessage(Flats.PREFIX + I18n.translate("error.no_permission"));
    }

    /**
     * Checks if the specified player has administrative permissions.
     *
     * <p>
     * This method verifies if the given player possesses the {@code ADMIN} permission.
     *
     * @param player The {@link Player} whose permissions are being checked. Must not be null.
     * @return {@code true} if the player has administrative permissions, {@code false} otherwise.
     */
    public static boolean hasAdminPermission(@NotNull Player player) {
        return player.hasPermission(ADMIN);
    }

    /**
     * Determines whether the specified player has permission to edit flats based on the current
     * configuration settings.
     * <p>
     * The permission check considers whether advanced permissions are enabled in the provided
     * configuration.
     *
     * @param player         The {@link Player} whose permissions are being checked. Must not be null.
     * @param settingsConfig The {@link SettingsConfig} containing the configuration for permission settings. Must not be null.
     * @return {@code true} if the player has the required permission to edit flats; {@code false} otherwise.
     */
    public static boolean canEditFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(EDIT_FLATS);
        } else {
            return player.hasPermission(ADMIN);
        }
    }

    /**
     * Determines whether the specified player is allowed to claim flats based on the given configuration.
     * <p>
     * The method checks the player's permissions if advanced permissions are enabled in the configuration.
     *
     * @param player The player whose permissions are being checked. Must not be {@code null}.
     * @param settingsConfig The configuration object that provides relevant settings. Must not be {@code null}.
     * @return {@code true} if the player is allowed to claim flats, {@code false} otherwise.
     */
    public static boolean canClaimFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(CLAIM_FLATS);
        } else {
            return true;
        }
    }

    /**
     * Checks whether the player is allowed to view flats based on the configured settings and permissions.
     * <p>
     * This method considers both the player's individual permissions and the advanced permissions setting
     * defined in {@link SettingsConfig}.
     *
     * @param player The {@link Player} whose permissions are being checked. Must not be null.
     * @param settingsConfig The {@link SettingsConfig} instance providing the configuration settings. Must not be null.
     * @return {@code true} if the player can view flats; {@code false} otherwise.
     */
    public static boolean canShowFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(SHOW_FLATS);
        } else {
            return true;
        }
    }

    /**
     * Checks whether a player has the necessary permissions to list flats.
     * <p>
     * Uses advanced permissions if enabled in the {@link SettingsConfig}.
     *
     * @param player The player whose permissions are being checked. Must not be null.
     * @param settingsConfig The configuration containing permission settings. Must not be null.
     * @return {@code true} if the player has the required permission to list flats; {@code false} otherwise.
     */
    public static boolean canListFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(LIST_FLATS);
        } else {
            return player.hasPermission(ADMIN);
        }
    }

    /**
     * Determines if the player is allowed to view information about flats based on permissions
     * and the configuration settings.
     * <p>
     * If advanced permissions are enabled in the settings, the player must have the appropriate
     * permission to access this functionality.
     *
     * @param player the {@link Player} whose permissions are being checked. Must not be null.
     * @param settingsConfig the {@link SettingsConfig} object that contains permission settings. Must not be null.
     * @return {@code true} if the player is allowed to view flat information; {@code false} otherwise.
     */
    public static boolean canInfoFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(INFO_FLATS);
        } else {
            return true;
        }
    }

    /**
     * Determines whether the specified player can be trusted based on the provided settings configuration.
     * <p>
     * Trust is granted either by default or based on the player's permission when advanced permissions are enabled.
     *
     * @param player The {@link Player} whose trustworthiness is being checked. Must not be null.
     * @param settingsConfig The {@link SettingsConfig} instance containing permission-related settings. Must not be null.
     * @return {@code true} if the player can be trusted; {@code false} otherwise.
     */
    public static boolean canTrustPlayers(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(TRUST_PLAYERS);
        } else {
            return true;
        }
    }

    /**
     * Determines whether the player can skip the command delay based on their permissions and the configuration settings.
     * <p>
     * This method checks if the player has the appropriate permission depending on the
     * advanced permissions setting in the {@link SettingsConfig}.
     *
     * @param player the {@link Player} whose permissions are being checked. Must not be null.
     * @param settingsConfig the {@link SettingsConfig} instance containing the configuration settings. Must not be null.
     * @return {@code true} if the player can skip the command delay, {@code false} otherwise.
     */
    public static boolean canSkipCommandDelay(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(SKIP_COMMAND_DELAY);
        } else {
            return player.hasPermission(ADMIN);
        }
    }

    /**
     * Checks if a player has no permissions related to flat management.
     * <p>
     * The permissions to edit, claim, show, list, view info, manage trust, and skip command delays
     * are evaluated based on the current settings.
     *
     * @param player The player whose permissions are being checked. Must not be null.
     * @param settingsConfig The settings configuration governing the permission checks. Must not be null.
     * @return {@code true} if the player has none of the relevant permissions; {@code false} otherwise.
     */
    public static boolean hasZeroPermissions(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        return !canEditFlats(player, settingsConfig) && !canClaimFlats(player, settingsConfig) &&
                !canShowFlats(player, settingsConfig) && !canListFlats(player, settingsConfig) &&
                !canInfoFlats(player, settingsConfig) && !canTrustPlayers(player, settingsConfig) &&
                !canSkipCommandDelay(player, settingsConfig);
    }

}
