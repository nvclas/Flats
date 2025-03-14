package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RemoveSubCommand implements SubCommand {
    
    private final Flats flatsPlugin;
    
    public RemoveSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }
    
    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("remove.usage"));
            return;
        }
        String flatToRemove = args[1];
        if (!flatsPlugin.getFlatsManager().getAllFlatNames().contains(flatToRemove)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_not_exist"));
            return;
        }
        flatsPlugin.getFlatsManager().delete(flatToRemove);
        player.sendMessage(Flats.PREFIX + I18n.translate("remove.success", flatToRemove));
    }
}
