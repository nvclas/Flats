package de.nvclas.flats.core.commands.subcommands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.commands.SubCommand;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permission;
import de.nvclas.flats.core.volumes.Area;
import de.nvclas.flats.core.volumes.Flat;
import de.nvclas.flats.core.volumes.Selection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AddSubCommand implements SubCommand {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;
    private final FlatsCache flatsCache;

    public AddSubCommand(BaseFlats plugin) {
        this.plugin = plugin;
        this.configAdapter = plugin.getConfigAdapter();
        this.flatsCache = plugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permission.canEditFlats(plugin, player, configAdapter)) {
            Permission.showNoPermissionMessage(plugin, player);
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("add.usage", plugin.getMainCommandName()));
            return;
        }

        Selection selection = Selection.getSelection(player);
        if (!isSelectionValid(player, selection) || doesSelectionIntersect(player, selection)) {
            return;
        }

        String flatName = args[1];
        if (flatName.length() > 64) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("add.name_too_long"));
            return;
        }

        Area area = Area.fromSelection(selection, flatName);
        Flat flat = flatsCache.getFlat(flatName);
        if (flat == null) {
            flatsCache.create(flatName, area);
            player.sendMessage(plugin.getPrefix() + I18n.translate("add.success", flatName));
            return;
        }
        flatsCache.addAreaToFlat(flat, area);
        player.sendMessage(plugin.getPrefix() + I18n.translate("add.area_added", flat.getName()));
    }

    private boolean isSelectionValid(Player player, Selection selection) {
        int volume = selection.calculateVolume();
        if (volume == 0) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("error.nothing_selected"));
            return false;
        }
        if (volume > configAdapter.getMaxFlatSize()) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("error.selection_too_large"));
            return false;
        }
        return true;
    }

    private boolean doesSelectionIntersect(Player player, Selection selection) {
        int minX = Math.min(selection.getPos1().getBlockX(), selection.getPos2().getBlockX());
        int maxX = Math.max(selection.getPos1().getBlockX(), selection.getPos2().getBlockX());
        int minZ = Math.min(selection.getPos1().getBlockZ(), selection.getPos2().getBlockZ());
        int maxZ = Math.max(selection.getPos1().getBlockZ(), selection.getPos2().getBlockZ());

        for (Area area : flatsCache.getAreasIntersecting(selection.getPos1().getWorld().getName(), minX, maxX, minZ,
                maxZ)) {
            if (selection.intersects(area)) {
                player.sendMessage(plugin.getPrefix() + I18n.translate("error.flat_intersect"));
                player.sendMessage(plugin.getPrefix() + I18n.translate("error.flat_intersect.details",
                        area.getFlatName(),
                        area.getLocationString()));
                return true;
            }
        }
        return false;
    }
}
