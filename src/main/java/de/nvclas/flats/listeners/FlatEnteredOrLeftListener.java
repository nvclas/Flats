package de.nvclas.flats.listeners;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.events.FlatEnteredOrLeftEvent;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class FlatEnteredOrLeftListener implements Listener {

    private final Flats flatsPlugin;

    public FlatEnteredOrLeftListener(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }

    @EventHandler
    public void onFlatEnteredOrLeft(@NotNull FlatEnteredOrLeftEvent event) {
        SettingsConfig settings = flatsPlugin.getSettingsConfig();
        Player player = event.getPlayer();

        if (!settings.isAutoGamemodeEnabled() || Permissions.hasAdminPermission(player)) {
            return;
        }

        Flat flat = event.getFlat();

        if (event.hasEntered() && (flat.isOwner(player) || flat.isTrusted(player))) {
            player.setGameMode(settings.getInsideGamemode());
        } else {
            player.setGameMode(settings.getOutsideGamemode());
        }
    }
}
