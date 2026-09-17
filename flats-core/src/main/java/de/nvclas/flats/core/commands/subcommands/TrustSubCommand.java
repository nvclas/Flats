package de.nvclas.flats.core.commands.subcommands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.commands.SubCommand;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.util.CommandUtils;
import de.nvclas.flats.core.util.FlatsCommandUtils;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permissions;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TrustSubCommand implements SubCommand {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;
    private final FlatsCache flatsCache;

    public TrustSubCommand(BaseFlats plugin) {
        this.plugin = plugin;
        this.configAdapter = plugin.getConfigAdapter();
        this.flatsCache = plugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canTrustPlayers(player, configAdapter)) {
            Permissions.showNoPermissionMessage(plugin, player);
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("trust.usage"));
            return;
        }
        Flat flat = FlatsCommandUtils.getOwnedFlatAtPlayerLocation(plugin, player, flatsCache);
        if (flat == null) {
            return;
        }
        OfflinePlayer target = CommandUtils.findOfflinePlayer(plugin, player, args[1]);
        if (target == null) {
            return;
        }
        if (flat.isTrusted(target)) {
            player.sendMessage(plugin.getPrefix() + I18n.translate("trust.already_trusted", target.getName()));
            return;
        }
        flat.addTrusted(target);
        flatsCache.save(flat);
        player.sendMessage(plugin.getPrefix() + I18n.translate("trust.success", target.getName()));
    }

}
