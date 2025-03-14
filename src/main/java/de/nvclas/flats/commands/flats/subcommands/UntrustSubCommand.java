package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.util.CommandUtils;
import de.nvclas.flats.util.FlatsCommandUtils;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UntrustSubCommand implements SubCommand {

    private final Flats flatsPlugin;

    public UntrustSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }
    
    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("untrust.usage"));
            return;
        }
        Flat flat = FlatsCommandUtils.getOwnedFlatAtPlayerLocation(player, flatsPlugin);
        if (flat == null) {
            return;
        }
        OfflinePlayer target = CommandUtils.findOfflinePlayer(player, args[1]);
        if (target == null) return;
        if (!flat.isTrusted(target)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("untrust.not_trusted", target.getName()));
            return;
        }
        flat.removeTrusted(target);
        player.sendMessage(Flats.PREFIX + I18n.translate("untrust.success", target.getName()));
    }

}
