package de.nvclas.flats.util;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.SettingsConfig;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for managing and checking player permissions.
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

    public static void showNoPermissionMessage(@NotNull Player player) {
        player.sendMessage(Flats.PREFIX + I18n.translate("error.no_permission"));
    }

    public static boolean hasAdminPermission(@NotNull Player player) {
        return player.hasPermission(ADMIN);
    }

    public static boolean canEditFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(EDIT_FLATS);
        } else {
            return player.hasPermission(ADMIN);
        }
    }

    public static boolean canClaimFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(CLAIM_FLATS);
        } else {
            return true;
        }
    }

    public static boolean canShowFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(SHOW_FLATS);
        } else {
            return true;
        }
    }

    public static boolean canListFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(LIST_FLATS);
        } else {
            return player.hasPermission(ADMIN);
        }
    }

    public static boolean canInfoFlats(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(INFO_FLATS);
        } else {
            return true;
        }
    }

    public static boolean canTrustPlayers(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(TRUST_PLAYERS);
        } else {
            return true;
        }
    }

    public static boolean canSkipCommandDelay(@NotNull Player player, @NotNull SettingsConfig settingsConfig) {
        if (settingsConfig.getAdvancedPermissions()) {
            return player.hasPermission(SKIP_COMMAND_DELAY);
        } else {
            return player.hasPermission(ADMIN);
        }
    }

}
