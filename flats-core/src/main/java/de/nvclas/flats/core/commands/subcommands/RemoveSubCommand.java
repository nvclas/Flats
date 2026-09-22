package de.nvclas.flats.core.commands.subcommands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.commands.SubCommand;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permission;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RemoveSubCommand implements SubCommand {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;
    private final FlatsCache flatsCache;

    public RemoveSubCommand(BaseFlats plugin) {
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
            player.sendMessage(plugin.getPrefix() + I18n.translate("remove.usage", plugin.getMainCommandName()));
            return;
        }
        Flat flatToRemove = flatsCache.getFlat(args[1]);
        if (flatToRemove == null) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("error.flat_not_exist"));
            return;
        }
        flatsCache.delete(flatToRemove);
        player.sendMessage(plugin.getPrefix() + I18n.translate("remove.success", flatToRemove.getName()));
    }
}
