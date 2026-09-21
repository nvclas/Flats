package de.nvclas.flats.core.commands.subcommands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.commands.SubCommand;
import de.nvclas.flats.core.updater.UpdateResult;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UpdateSubCommand implements SubCommand {

    private final BaseFlats plugin;

    public UpdateSubCommand(@NotNull BaseFlats plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.hasAdminPermission(plugin, player)) {
            Permissions.showNoPermissionMessage(plugin, player);
            return;
        }

        plugin.getUpdateService().updateAsync()
                .thenAccept(result -> Bukkit.getScheduler()
                        .runTask(plugin, () -> sendStatusMessage(player, result)));
    }

    private void sendStatusMessage(@NotNull Player player, @NotNull UpdateResult result) {
        switch (result.status()) {
            case SUCCESS -> player.sendMessage(
                    plugin.getPrefix() + I18n.translate("update.success", result.fileName()));
            case UPDATE_AVAILABLE -> {
                if (result.downloadUrl() != null) {
                    player.sendMessage(
                            plugin.getPrefix() + I18n.translate("update.available_link", result.latestVersion(),
                                    result.downloadUrl()));
                } else {
                    player.sendMessage(plugin.getPrefix() + I18n.translate("update.available", result.latestVersion()));
                }
            }
            case NOT_FOUND ->
                    player.sendMessage(plugin.getPrefix() + I18n.translate("update.not_found", plugin.getPluginName()));
            case FAILED ->
                    player.sendMessage(plugin.getPrefix() + I18n.translate("update.failed", plugin.getPluginName()));
            case ALREADY_UP_TO_DATE ->
                    player.sendMessage(plugin.getPrefix() + I18n.translate("update.already_up_to_date"));
        }
    }
}
