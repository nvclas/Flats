package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.Flats;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

public class EntityDamageListener implements Listener {

    private final Flats flatsPlugin;

    public EntityDamageListener(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }

    @EventHandler
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        Flat flat = flatsPlugin.getFlatsCache().getFlatByLocation(event.getEntity().getLocation());
        Entity damager = event.getDamager();
        EventCancelChecker.cancelEventIfPlayerNotTrustedOrOwner(event, flat, damager);
    }
}