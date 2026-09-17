package de.nvclas.flats.lite.listeners.protection;

import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.volumes.Flat;
import de.nvclas.flats.lite.Flats;
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
        Flat flat = flatsCache.getFlatAtLocation(event.getEntity().getLocation());
        EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getRemover());
    }

}
