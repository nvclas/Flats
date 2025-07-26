package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    private final FlatsCache flatsCache;

    public BlockBreakListener(Flats flatsPlugin) {
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Flat flat = flatsCache.getFlatByLocation(event.getBlock().getLocation());
        EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getPlayer());
    }

}
