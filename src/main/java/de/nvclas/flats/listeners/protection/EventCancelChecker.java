package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

@UtilityClass
public class EventCancelChecker {

    public void cancelEventIfPlayerNotTrustedOrOwner(Cancellable event, Flat flat, Entity entity) {
        if (flat == null || entity.hasPermission(Permissions.ADMIN)) {
            return;
        }

        if (!(entity instanceof Player player) || (!flat.isOwner(player) && !flat.isTrusted(player))) {
            event.setCancelled(true);
        }

    }

}