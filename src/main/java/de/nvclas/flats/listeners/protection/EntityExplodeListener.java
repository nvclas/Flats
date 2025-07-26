package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class EntityExplodeListener implements Listener {

    private final FlatsCache flatsCache;

    public EntityExplodeListener(Flats flatsPlugin) {
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            Flat flat = flatsCache.getFlatByLocation(block.getLocation());
            if (flat != null) {
                event.blockList().remove(block);
            }
        }
    }

}
