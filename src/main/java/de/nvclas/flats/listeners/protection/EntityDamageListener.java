package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.managers.FlatsManager;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

public class EntityDamageListener implements Listener {
    @EventHandler
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        Flat flat = FlatsManager.getFlatByLocation(event.getEntity().getLocation());
        Entity damager = event.getDamager();
        if (flat == null || damager.hasPermission(Permissions.ADMIN)) {
            return;
        }

        if (!(damager instanceof Player) || !flat.isOwner((OfflinePlayer) damager)) {
            event.setCancelled(true);
        }
    }
}