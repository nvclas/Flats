package de.nvclas.flats.util;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.volumes.Flat;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class providing helper methods related to flat commands and operations
 * in the Flats plugin.
 */
@UtilityClass
public class FlatsCommandUtils {

    /**
     * Retrieves the {@link Flat} owned by the given {@link Player} at the player's current location.
     * If the player is not within a flat or does not own the flat at their location, an appropriate
     * error message is sent to the player, and {@code null} is returned.
     *
     * @param player     The {@link Player} whose owned flat at their current location is to be retrieved.
     *                   Must not be null.
     * @param flatsCache The {@link FlatsCache} instance used to fetch flats by location. Must not be null.
     * @return The {@link Flat} owned by the player at their current location, or {@code null} if the
     * player is not in a flat or does not own the flat at their location.
     */
    public @Nullable Flat getOwnedFlatAtPlayerLocation(Player player, FlatsCache flatsCache) {
        Flat flat = flatsCache.getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.not_in_flat"));
            return null;
        }
        if (!flat.isOwner(player)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.not_your_flat"));
            return null;
        }
        return flat;
    }

}
