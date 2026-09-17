package de.nvclas.flats.lite.listeners.protection;

import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.volumes.Flat;
import de.nvclas.flats.lite.Flats;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageByEntityListener implements Listener {

    private final FlatsCache flatsCache;

    public EntityDamageByEntityListener(Flats flatsPlugin) {
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Flat flat = flatsCache.getFlatAtLocation(event.getEntity().getLocation());
        EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getDamager());
    }

}
