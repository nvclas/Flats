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

public class ListSubCommand implements SubCommand {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;
    private final FlatsCache flatsCache;

    public ListSubCommand(BaseFlats plugin) {
        this.plugin = plugin;
        this.configAdapter = plugin.getConfigAdapter();
        this.flatsCache = plugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canListFlats(player, configAdapter)) {
            Permissions.showNoPermissionMessage(plugin, player);
            return;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getPrefix() + I18n.translate("list.invalid_page"));
                return;
            }
        }

        int pageSize = 5;
        int totalFlats = flatsCache.getTotalFlatsCount();
        if (totalFlats == 0) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("list.empty"));
            return;
        }

        int maxPages = (int) Math.ceil((double) totalFlats / pageSize);
        if (page < 1 || page > maxPages) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("list.page_not_found", page, maxPages));
            return;
        }

        player.sendMessage(plugin.getPrefix() + I18n.translate("list.title_page", page, maxPages));
        int offset = (page - 1) * pageSize;
        for (String name : flatsCache.getPaginatedFlatNames(offset, pageSize)) {
            Flat flat = flatsCache.getFlat(name);
            if (flat == null) {
                continue;
            }

            player.sendMessage(plugin.getPrefix() + I18n.translate("info.flat", flat.getName()));
            if (!flat.hasOwner()) {
                player.sendMessage(plugin.getPrefix() + I18n.translate("info.unoccupied"));
            } else {
                player.sendMessage(plugin.getPrefix() + I18n.translate("info.owner", flat.getOwner().getName()));
            }
        }
    }

}
