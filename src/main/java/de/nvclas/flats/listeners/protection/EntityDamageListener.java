package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

public class EntityDamageListener implements Listener {

    private final FlatsCache flatsCache;

    public EntityDamageListener(Flats flatsPlugin) {
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @EventHandler
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        Flat flat = flatsCache.getFlatByLocation(event.getEntity().getLocation());
        Entity damager = event.getDamager();
        EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, damager);
    }
}
