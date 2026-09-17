package de.nvclas.flats.lite.listeners.protection;

import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.volumes.Flat;
import de.nvclas.flats.lite.Flats;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class EntityChangeBlockListener implements Listener {

    private final FlatsCache flatsCache;

    public EntityChangeBlockListener(Flats flatsPlugin) {
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Flat flat = flatsCache.getFlatAtLocation(event.getBlock().getLocation());
        EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getEntity());
    }

}
