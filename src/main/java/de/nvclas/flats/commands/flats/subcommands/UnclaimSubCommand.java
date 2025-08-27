package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.FlatsCommandUtils;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UnclaimSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;
    private static final Map<UUID, Long> confirmationMap = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 10000; // 10 seconds

    public UnclaimSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canClaimFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }
        
        Flat flat = FlatsCommandUtils.getOwnedFlatAtPlayerLocation(player, flatsCache);
        if (flat == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        Long lastConfirmation = confirmationMap.get(playerId);
        long currentTime = System.currentTimeMillis();
        
        // Check if player has already started confirmation process and it's still valid
        if (lastConfirmation != null && (currentTime - lastConfirmation) < CONFIRMATION_TIMEOUT) {
            // Execute the unclaim
            player.sendMessage(Flats.PREFIX + I18n.translate("unclaim.success"));
            flat.setOwner(null);
            flat.getTrusted().clear();
            confirmationMap.remove(playerId);
        } else {
            // Start confirmation process
            confirmationMap.put(playerId, currentTime);
            player.sendMessage(Flats.PREFIX + I18n.translate("unclaim.confirmation", flat.getName()));
            player.sendMessage(Flats.PREFIX + I18n.translate("unclaim.confirmation.timeout"));
        }
    }
}
