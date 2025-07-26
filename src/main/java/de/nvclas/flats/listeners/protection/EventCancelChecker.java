package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class EventCancelChecker {

    public void cancelEventIfPlayerNotTrustedOrOwner(@NotNull Cancellable event, Flat flat, @NotNull Entity entity) {
        if (flat == null || entity.hasPermission(Permissions.ADMIN)) {
            return;
        }

        if (!(entity instanceof Player player) || (!flat.isOwner(player) && !flat.isTrusted(player))) {
            event.setCancelled(true);
        }

    }

}
