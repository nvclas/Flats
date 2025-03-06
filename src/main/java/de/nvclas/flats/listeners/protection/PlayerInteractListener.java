package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.Flats;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerInteractListener implements Listener {

    private final Flats flatsPlugin;

    public PlayerInteractListener(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }

    @EventHandler
    public void onBlockPlace(@NotNull PlayerInteractEvent event) {
        if (event.getInteractionPoint() == null) return;
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(event.getInteractionPoint());
        Player player = event.getPlayer();
        if (flat == null || player.hasPermission(Permissions.ADMIN)) {
            return;
        }

        if (!flat.isOwner(player) && !flat.isTrusted(player)) {
            event.setCancelled(true);
        }
    }
}