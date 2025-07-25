package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.FlatsCommandUtils;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnclaimSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public UnclaimSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canClaimFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }
        Flat flat = FlatsCommandUtils.getOwnedFlatAtPlayerLocation(player, flatsCache);
        if (flat == null) {
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("unclaim.success"));
        flat.setOwner(null);
        flat.getTrusted().clear();
    }
}
