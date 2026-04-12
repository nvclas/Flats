package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InfoSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public InfoSubCommand(Flats flatsPlugin) {
        settingsConfig = flatsPlugin.getSettingsConfig();
        flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canInfoFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }

        if (args.length == 1) {
            Area area = flatsCache.getAreaAtLocation(player.getLocation());
            if (area != null) {
                sendFlatInfo(player, area);
                return;
            }
            player.sendMessage(Flats.PREFIX + I18n.translate("error.not_in_flat"));
            return;
        }

        Flat flat = flatsCache.getFlat(args[1]);
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_not_exist"));
            return;
        }
        sendFlatInfo(player, flat);
    }

    private void sendFlatInfo(Player player, Flat flat) {
        player.sendMessage(Flats.PREFIX + I18n.translate("info.flat", flat.getName()));
        sendOwnerInfo(player, flat);
        sendTrustedPlayersInfo(player, flat);
        sendAreaInfo(player, flat);
    }

    private void sendFlatInfo(Player player, Area area) {
        Flat flat = flatsCache.getExistingFlat(area.getFlatName());
        sendFlatInfo(player, flat);
        player.sendMessage(Flats.PREFIX + I18n.translate("info.area", area.getLocationString()));
    }

    private void sendOwnerInfo(Player player, Flat flat) {
        OfflinePlayer owner = flat.getOwner();
        if (owner == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate("info.unoccupied"));
        } else {
            player.sendMessage(Flats.PREFIX + I18n.translate("info.owner", owner.getName()));
        }
    }

    private void sendTrustedPlayersInfo(Player player, Flat flat) {
        if (flat.getTrusted().isEmpty()) {
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("info.trusted_header"));
        for (OfflinePlayer trustedPlayer : flat.getTrusted()) {
            String messageKey = flat.getTrusted()
                    .getLast() == trustedPlayer ? "info.trusted_last" : "info.trusted_item";
            player.sendMessage(Flats.PREFIX + I18n.translate(messageKey, trustedPlayer.getName()));
        }
    }

    private void sendAreaInfo(Player player, Flat flat) {
        if (flat.getAreas().isEmpty()) {
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("info.areas_header"));
        for (Area area : flat.getAreas()) {
            String messageKey = flat.getAreas()
                    .getLast() == area ? "info.areas_last" : "info.areas_item";
            player.sendMessage(Flats.PREFIX + I18n.translate(messageKey, area.getLocationString()));
        }
    }
}
