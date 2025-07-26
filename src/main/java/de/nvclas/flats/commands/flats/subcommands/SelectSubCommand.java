package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.util.Permissions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SelectSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;

    public SelectSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canEditFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }
        player.getInventory().addItem(SelectionItem.getItem());
    }
}
