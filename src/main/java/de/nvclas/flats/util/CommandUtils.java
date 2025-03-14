package de.nvclas.flats.util;

import de.nvclas.flats.Flats;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class that provides methods for working with commands and player-related operations.
 */
@UtilityClass
public class CommandUtils {

    /**
     * Finds an offline player by name, if the player is cached.
     * Sends an error message to the command sender if the player is not found.
     *
     * @param commandSender     The {@link Player} sending the command. Must not be null.
     * @param offlinePlayerName The name of the offline player to search for. Must not be null.
     * @return The {@link OfflinePlayer} if found, or {@code null} if the player is not cached.
     */
    public @Nullable OfflinePlayer findOfflinePlayer(Player commandSender, String offlinePlayerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(offlinePlayerName);
        if (target == null) {
            commandSender.sendMessage(Flats.PREFIX + I18n.translate("error.player_not_found", offlinePlayerName));
            return null;
        }
        return target;
    }

}
