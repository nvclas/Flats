package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.selection.Flat;
import de.nvclas.flats.utils.Permissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerInteractListener implements Listener {
    @EventHandler
    public void onBlockPlace(@NotNull PlayerInteractEvent event) {
        Flat flat = Flat.getFlatByLocation(event.getInteractionPoint());
        Player player = event.getPlayer();
        if (flat == null || player.hasPermission(Permissions.ADMIN)) {
            return;
        }

        if (!flat.isOwner(player)) {
            event.setCancelled(true);
        }
    }
}