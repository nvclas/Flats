package de.nvclas.flats.core.listeners;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.events.FlatEnteredOrLeftEvent;
import de.nvclas.flats.core.util.Permission;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class FlatEnteredOrLeftListener implements Listener {

    private final BaseFlats plugin;

    public FlatEnteredOrLeftListener(BaseFlats plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFlatEnteredOrLeft(@NotNull FlatEnteredOrLeftEvent event) {
        ConfigAdapter configAdapter = plugin.getConfigAdapter();
        Player player = event.getPlayer();

        if (!configAdapter.isAutoGamemodeEnabled() || Permission.hasAdminPermission(plugin, player)) {
            return;
        }

        Flat flat = event.getFlat();

        if (event.hasEntered()) {
            if (flat.isOwner(player) || flat.isTrusted(player)) {
                player.setGameMode(configAdapter.getInsideGamemode());
            }
            return;
        }
        player.setGameMode(configAdapter.getOutsideGamemode());
    }
}
