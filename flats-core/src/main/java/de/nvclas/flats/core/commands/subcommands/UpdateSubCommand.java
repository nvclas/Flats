package de.nvclas.flats.core.commands.subcommands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.commands.SubCommand;
import de.nvclas.flats.core.updater.UpdateDownloader;
import de.nvclas.flats.core.updater.UpdateStatus;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UpdateSubCommand implements SubCommand {

    private final BaseFlats plugin;

    public UpdateSubCommand(BaseFlats plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.hasAdminPermission(plugin, player)) {
            Permissions.showNoPermissionMessage(plugin, player);
            return;
        }
        UpdateDownloader updateDownloader = new UpdateDownloader(plugin,
                "https://api.github.com/repos/nvclas/Flats/releases/latest");
        updateDownloader.downloadLatestReleaseAsync()
                .thenAccept(status -> Bukkit.getScheduler()
                        .runTask(plugin, () -> sendStatusMessage(player, updateDownloader, status)));
    }

    private void sendStatusMessage(@NotNull Player player, @NotNull UpdateDownloader updateDownloader,
            @NotNull UpdateStatus status) {
        switch (status) {
            case SUCCESS -> player.sendMessage(
                    plugin.getPrefix() + I18n.translate("update.success", updateDownloader.getFileName()));
            case NOT_FOUND ->
                    player.sendMessage(plugin.getPrefix() + I18n.translate("update.not_found", plugin.getPluginName()));
            case FAILED ->
                    player.sendMessage(plugin.getPrefix() + I18n.translate("update.failed", plugin.getPluginName()));
            case ALREADY_UP_TO_DATE ->
                    player.sendMessage(plugin.getPrefix() + I18n.translate("update.already_up_to_date"));
        }
    }
}
