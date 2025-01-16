package de.nvclas.flats.listeners;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.events.FlatEnteredOrLeftEvent;
import de.nvclas.flats.selection.Flat;
import de.nvclas.flats.utils.Permissions;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class FlatEnteredOrLeftListener implements Listener {
    @EventHandler
    public void onFlatEnteredOrLeft(@NotNull FlatEnteredOrLeftEvent event) {
        SettingsConfig settings = Flats.getInstance().getSettingsConfig();
        Player player = event.getPlayer();
        
        if (!settings.isAutoGamemodeEnabled() || player.hasPermission(Permissions.ADMIN)) {
            return;
        }
        
        Flat flat = event.getFlat();
        
        if (event.hasEntered() && flat.isOwner(player)) {
            player.setGameMode(GameMode.valueOf(settings.getInsideGamemode().toUpperCase()));
        } else {
            player.setGameMode(GameMode.valueOf(settings.getOutsideGamemode().toUpperCase()));
        }
    }
}
