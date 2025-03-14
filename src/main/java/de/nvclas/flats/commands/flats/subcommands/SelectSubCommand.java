package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.util.Permissions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SelectSubCommand implements SubCommand {
    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        player.getInventory().addItem(SelectionItem.getItem());
    }
}
