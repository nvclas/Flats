package de.nvclas.flats.core.commands.subcommands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.commands.SubCommand;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permissions;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimSubCommand implements SubCommand {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;
    private final FlatsCache flatsCache;

    public ClaimSubCommand(BaseFlats plugin) {
        this.plugin = plugin;
        this.configAdapter = plugin.getConfigAdapter();
        this.flatsCache = plugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        Flat flat = flatsCache.getFlatAtLocation(player.getLocation());
        if (!Permissions.canClaimFlats(plugin, player, configAdapter)) {
            Permissions.showNoPermissionMessage(plugin, player);
            return;
        }
        if (flat == null) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("error.not_in_flat"));
            return;
        }
        if (flat.isOwner(player)) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("claim.already_your_flat"));
            return;
        }
        if (flat.hasOwner()) {
            player.sendMessage(
                    plugin.getPrefix() + I18n.translate("claim.already_owned_by", flat.getOwner().getName()));
            return;
        }
        if (!Permissions.hasAdminPermission(plugin, player) &&
                flatsCache.getOwnedFlatsCount(player) >= configAdapter.getMaxClaimableFlats()) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("claim.max_claimable_flats_reached",
                    configAdapter.getMaxClaimableFlats()));
            return;
        }
        flat.setOwner(player);
        flatsCache.save(flat);
        player.sendMessage(plugin.getPrefix() + I18n.translate("claim.success"));
    }
}
