package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Selection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AddSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public AddSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canEditFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("add.usage"));
            return;
        }

        Selection selection = Selection.getSelection(player);
        if (!isSelectionValid(player, selection) || doesSelectionIntersect(player, selection)) {
            return;
        }

        String flatName = args[1];
        Area area = Area.fromSelection(selection, flatName);

        if (!flatsCache.existsFlat(flatName)) {
            flatsCache.create(flatName, area);
            player.sendMessage(Flats.PREFIX + I18n.translate("add.success", flatName));
            return;
        }
        flatsCache.getExistingFlat(flatName).addArea(area);
        player.sendMessage(Flats.PREFIX + I18n.translate("add.area_added", flatName));
    }

    private boolean isSelectionValid(Player player, Selection selection) {
        int volume = selection.calculateVolume();
        if (volume == 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.nothing_selected"));
            return false;
        }
        if (volume > settingsConfig.getMaxFlatSize()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.selection_too_large"));
            return false;
        }
        return true;
    }

    private boolean doesSelectionIntersect(Player player, Selection selection) {
        return flatsCache.getAllAreas().stream().filter(selection::intersects).findFirst().map(area -> {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_intersect"));
            player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_intersect.details",
                    area.getFlatName(),
                    area.getLocationString()));
            return true;
        }).orElse(false);
    }
}
