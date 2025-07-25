package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RemoveSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public RemoveSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canEditFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("remove.usage"));
            return;
        }
        String flatToRemove = args[1];
        if (!flatsCache.getAllFlatNames().contains(flatToRemove)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_not_exist"));
            return;
        }
        flatsCache.delete(flatToRemove);
        player.sendMessage(Flats.PREFIX + I18n.translate("remove.success", flatToRemove));
    }
}
