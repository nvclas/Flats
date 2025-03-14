package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimSubCommand implements SubCommand {

    private final Flats flatsPlugin;
    
    public ClaimSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }
    
    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.not_in_flat"));
            return;
        }
        if (flat.isOwner(player)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("claim.already_your_flat"));
            return;
        }
        if (flat.hasOwner()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("claim.already_owned_by", flat.getOwner().getName()));
            return;
        }
        flat.setOwner(player);
        player.sendMessage(Flats.PREFIX + I18n.translate("claim.success"));
    }
}
