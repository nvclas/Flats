package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimSubCommand implements SubCommand {

    private final Flats flatsPlugin;
    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public ClaimSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        Flat flat = flatsPlugin.getFlatsCache().getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.not_in_flat"));
            return;
        }
        if (flat.isOwner(player)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("claim.already_your_flat"));
            return;
        }
        if (flat.hasOwner()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("claim.already_owned_by", flat.getOwner().getName()));
            return;
        }
        if (!player.hasPermission(Permissions.ADMIN) && flatsCache.getOwnedFlatsCount(player) >= settingsConfig.getMaxClaimableFlats()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("claim.max_claimable_flats_reached",
                    flatsPlugin.getSettingsConfig().getMaxClaimableFlats()));
            return;
        }
        flat.setOwner(player);
        player.sendMessage(Flats.PREFIX + I18n.translate("claim.success"));
    }
}
