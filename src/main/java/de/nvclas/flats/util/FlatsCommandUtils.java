package de.nvclas.flats.util;

import de.nvclas.flats.Flats;
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
     * Retrieves the {@link Flat} owned by the specified {@link Player} at their current location.
     * <p>
     * If the player is not within any flat, or the flat at the player's location is not owned
     * by the player, the method sends appropriate error messages to the player and returns {@code null}.
     *
     * @param player      the {@link Player} whose location is used to find the flat. Must not be null.
     * @param flatsPlugin the {@link Flats} plugin instance used to access the flats management system. Must not be null.
     * @return the {@link Flat} owned by the player at their current location, or {@code null} if the player is not in a flat or does not own the flat.
     */
    public @Nullable Flat getOwnedFlatAtPlayerLocation(Player player, Flats flatsPlugin) {
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
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
