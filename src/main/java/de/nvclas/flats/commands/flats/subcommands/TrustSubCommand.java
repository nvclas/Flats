package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.CommandUtils;
import de.nvclas.flats.util.FlatsCommandUtils;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TrustSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public TrustSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canTrustPlayers(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("trust.usage"));
            return;
        }
        Flat flat = FlatsCommandUtils.getOwnedFlatAtPlayerLocation(player, flatsCache);
        if (flat == null) {
            return;
        }
        OfflinePlayer target = CommandUtils.findOfflinePlayer(player, args[1]);
        if (target == null) return;
        if (flat.isTrusted(target)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("trust.already_trusted", target.getName()));
            return;
        }
        flat.addTrusted(target);
        player.sendMessage(Flats.PREFIX + I18n.translate("trust.success", target.getName()));
    }

}
