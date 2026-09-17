package de.nvclas.flats.core.commands.subcommands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.commands.SubCommand;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.items.SelectionItem;
import de.nvclas.flats.core.util.Permissions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SelectSubCommand implements SubCommand {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;

    public SelectSubCommand(BaseFlats plugin) {
        this.plugin = plugin;
        this.configAdapter = plugin.getConfigAdapter();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canEditFlats(player, configAdapter)) {
            Permissions.showNoPermissionMessage(plugin, player);
            return;
        }
        player.getInventory().addItem(SelectionItem.getItem(plugin));
    }
}
