package de.nvclas.flats.lite.listeners.protection;

import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.volumes.Flat;
import de.nvclas.flats.lite.Flats;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    private final FlatsCache flatsCache;

    public BlockPlaceListener(Flats flatsPlugin) {
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Flat flat = flatsCache.getFlatAtLocation(event.getBlock().getLocation());
        EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getPlayer());
    }

}
