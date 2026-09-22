package de.nvclas.flats.core.commands.subcommands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.commands.SubCommand;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.util.FlatsCommandUtils;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permission;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnclaimSubCommand implements SubCommand {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;
    private final FlatsCache flatsCache;

    public UnclaimSubCommand(BaseFlats plugin) {
        this.plugin = plugin;
        this.configAdapter = plugin.getConfigAdapter();
        this.flatsCache = plugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permission.canClaimFlats(plugin, player, configAdapter)) {
            Permission.showNoPermissionMessage(plugin, player);
            return;
        }
        Flat flat = FlatsCommandUtils.getOwnedFlatAtPlayerLocation(plugin, player, flatsCache);
        if (flat == null) {
            return;
        }
        player.sendMessage(plugin.getPrefix() + I18n.translate("unclaim.success"));
        flat.setOwner(null);
        flat.getTrusted().clear();
        flatsCache.save(flat);
    }
}
