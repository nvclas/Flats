package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InfoSubCommand implements SubCommand {
    
    private final Flats flatsPlugin;
    
    public InfoSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }
    
    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        for (Area area : flatsPlugin.getFlatsManager().getAllAreas()) {
            if (area.isWithinBounds(player.getLocation())) {
                player.sendMessage(Flats.PREFIX + I18n.translate("info.flat", area.getFlatName()));
                OfflinePlayer owner = flatsPlugin.getFlatsManager().getFlat(area.getFlatName()).getOwner();
                if (owner == null) {
                    player.sendMessage(Flats.PREFIX + I18n.translate("info.unoccupied"));
                } else {
                    player.sendMessage(Flats.PREFIX + I18n.translate("info.owner", owner.getName()));
                }
                listAllTrustedOfFlat(player, flatsPlugin.getFlatsManager().getFlat(area.getFlatName()));
                player.sendMessage(Flats.PREFIX + I18n.translate("info.area", area.getLocationString()));
                return;
            }
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("error.not_in_flat"));
    }

    private void listAllTrustedOfFlat(Player player, Flat flat) {
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
    
}
