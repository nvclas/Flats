package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.Flats;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    private final Flats flatsPlugin;

    public PlayerInteractListener(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if(event.getClickedBlock() != null) {
            Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(event.getClickedBlock().getLocation());
            EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, player);
            return;
        }
        if(event.getInteractionPoint() != null) {
            Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(event.getInteractionPoint());
            EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, player);
        }
    }
}
