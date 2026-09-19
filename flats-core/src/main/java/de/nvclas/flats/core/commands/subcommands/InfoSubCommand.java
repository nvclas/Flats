package de.nvclas.flats.core.commands.subcommands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.commands.SubCommand;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permissions;
import de.nvclas.flats.core.volumes.Area;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InfoSubCommand implements SubCommand {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;
    private final FlatsCache flatsCache;

    public InfoSubCommand(BaseFlats plugin) {
        this.plugin = plugin;
        configAdapter = plugin.getConfigAdapter();
        flatsCache = plugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canInfoFlats(plugin, player, configAdapter)) {
            Permissions.showNoPermissionMessage(plugin, player);
            return;
        }

        if (args.length == 1) {
            Area area = flatsCache.getAreaAtLocation(player.getLocation());
            if (area != null) {
                sendFlatInfo(player, area);
                return;
            }
            player.sendMessage(plugin.getPrefix() + I18n.translate("error.not_in_flat"));
            return;
        }

        Flat flat = flatsCache.getFlat(args[1]);
        if (flat == null) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("error.flat_not_exist"));
            return;
        }
        sendFlatInfo(player, flat);
    }

    private void sendFlatInfo(Player player, Flat flat) {
        player.sendMessage(plugin.getPrefix() + I18n.translate("info.flat", flat.getName()));
        sendOwnerInfo(player, flat);
        sendTrustedPlayersInfo(player, flat);
        sendAreaInfo(player, flat);
    }

    private void sendFlatInfo(Player player, Area area) {
        Flat flat = flatsCache.getExistingFlat(area.getFlatName());
        sendFlatInfo(player, flat);
        player.sendMessage(plugin.getPrefix() + I18n.translate("info.area", area.getLocationString()));
    }

    private void sendOwnerInfo(Player player, Flat flat) {
        OfflinePlayer owner = flat.getOwner();
        if (owner == null) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("info.unoccupied"));
        } else {
            player.sendMessage(plugin.getPrefix() + I18n.translate("info.owner", owner.getName()));
        }
    }

    private void sendTrustedPlayersInfo(Player player, Flat flat) {
        if (flat.getTrusted().isEmpty()) {
            return;
        }
        player.sendMessage(plugin.getPrefix() + I18n.translate("info.trusted_header"));
        for (OfflinePlayer trustedPlayer : flat.getTrusted()) {
            String messageKey = flat.getTrusted()
                    .getLast() == trustedPlayer ? "info.trusted_last" : "info.trusted_item";
            player.sendMessage(plugin.getPrefix() + I18n.translate(messageKey, trustedPlayer.getName()));
        }
    }

    private void sendAreaInfo(Player player, Flat flat) {
        if (flat.getAreas().isEmpty()) {
            return;
        }
        player.sendMessage(plugin.getPrefix() + I18n.translate("info.areas_header"));
        for (Area area : flat.getAreas()) {
            String messageKey = flat.getAreas()
                    .getLast() == area ? "info.areas_last" : "info.areas_item";
            player.sendMessage(plugin.getPrefix() + I18n.translate(messageKey, area.getLocationString()));
        }
    }
}
