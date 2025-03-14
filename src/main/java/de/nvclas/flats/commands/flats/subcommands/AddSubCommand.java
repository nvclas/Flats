package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Selection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AddSubCommand implements SubCommand {
    
    private final Flats flatsPlugin;
    
    public AddSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }
    
    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("add.usage"));
            return;
        }

        Selection selection = Selection.getSelection(player);
        int volume = selection.calculateVolume();
        if (volume == 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.nothing_selected"));
            return;
        }
        if (volume > flatsPlugin.getSettingsConfig().getMaxFlatSize()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.selection_too_large"));
            return;
        }
        if (doesSelectionIntersect(player, selection)) {
            return;
        }
        String flatName = args[1];
        if (!flatsPlugin.getFlatsManager().existsFlat(flatName)) {
            flatsPlugin.getFlatsManager().create(flatName, Area.fromSelection(selection, flatName));
            player.sendMessage(Flats.PREFIX + I18n.translate("add.success", flatName));
            return;
        }
        flatsPlugin.getFlatsManager().addArea(flatName, Area.fromSelection(selection, flatName));
        player.sendMessage(Flats.PREFIX + I18n.translate("add.area_added", flatName));
    }

    private boolean doesSelectionIntersect(Player player, Selection selection) {
        return flatsPlugin.getFlatsManager()
                .getAllAreas()
                .stream()
                .filter(selection::intersects)
                .findFirst()
                .map(area -> {
                    player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_intersect"));
                    player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_intersect.details",
                            area.getFlatName(),
                            area.getLocationString()));
                    return true;
                })
                .orElse(false);
    }
}
