package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ListSubCommand implements SubCommand {

    private final Flats flatsPlugin;

    public ListSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (flatsPlugin.getFlatsCache().getAllFlatNames().isEmpty()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("list.empty"));
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("list.title"));
        for (Flat flat : flatsPlugin.getFlatsCache().getAllFlats()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("info.flat", flat.getName()));
            if (!flat.hasOwner()) {
                player.sendMessage(Flats.PREFIX + I18n.translate("info.unoccupied"));
            } else {
                player.sendMessage(Flats.PREFIX + I18n.translate("info.owner", flat.getOwner().getName()));
            }
            listAllAreasOfFlat(player, flat);
        }
    }

    private void listAllAreasOfFlat(Player player, Flat flat) {
        player.sendMessage(Flats.PREFIX + I18n.translate("list.areas_header"));
        for (Area area : flat.getAreas()) {
            String messageKey = flat.getAreas().getLast() == area ? "list.areas_last" : "list.areas_item";
            player.sendMessage(Flats.PREFIX + I18n.translate(messageKey, area.getLocationString()));
        }
    }
}
