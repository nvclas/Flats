package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

public class HangingBreakByEntityListener implements Listener {

    private final FlatsCache flatsCache;

    public HangingBreakByEntityListener(Flats flatsPlugin) {
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Flat flat = flatsCache.getFlatByLocation(event.getEntity().getLocation());
        EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getRemover());
    }

}
