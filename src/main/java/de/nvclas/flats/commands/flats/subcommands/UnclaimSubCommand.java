package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.util.FlatsCommandUtils;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnclaimSubCommand implements SubCommand {

    private final Flats flatsPlugin;

    public UnclaimSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        Flat flat = FlatsCommandUtils.getOwnedFlatAtPlayerLocation(player, flatsPlugin);
        if (flat == null) {
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("unclaim.success"));
        flat.setOwner(null);
        flat.getTrusted().clear();
    }
}
