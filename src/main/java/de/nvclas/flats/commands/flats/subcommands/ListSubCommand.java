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

public class ListSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public ListSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canListFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Flats.PREFIX + I18n.translate("list.invalid_page"));
                return;
            }
        }

        int pageSize = 5;
        int totalFlats = flatsCache.getTotalFlatsCount();
        if (totalFlats == 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("list.empty"));
            return;
        }

        int maxPages = (int) Math.ceil((double) totalFlats / pageSize);
        if (page < 1 || page > maxPages) {
            player.sendMessage(Flats.PREFIX + I18n.translate("list.page_not_found", page, maxPages));
            return;
        }

        player.sendMessage(Flats.PREFIX + I18n.translate("list.title_page", page, maxPages));
        int offset = (page - 1) * pageSize;
        for (String name : flatsCache.getPaginatedFlatNames(offset, pageSize)) {
            Flat flat = flatsCache.getFlat(name);
            if (flat == null)
                continue;

            player.sendMessage(Flats.PREFIX + I18n.translate("info.flat", flat.getName()));
            if (!flat.hasOwner()) {
                player.sendMessage(Flats.PREFIX + I18n.translate("info.unoccupied"));
            } else {
                player.sendMessage(Flats.PREFIX + I18n.translate("info.owner", flat.getOwner().getName()));
            }
        }
    }

}
