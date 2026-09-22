package de.nvclas.flats.core.listeners;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.util.Permission;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the protection of flats
 */
public class ProtectionListener implements Listener {

    private final BaseFlats plugin;
    private final FlatsCache flatsCache;

    public ProtectionListener(BaseFlats plugin) {
        this.plugin = plugin;
        this.flatsCache = plugin.getFlatsCache();
    }

    private void cancelEventIfPlayerNotTrustedOrOwner(@NotNull Cancellable event, Flat flat, @NotNull Entity entity) {
        if (flat == null) {
            return;
        }
        if (!(entity instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if (!Permission.hasAdminPermission(plugin, player) && !flat.isOwner(player) && !flat.isTrusted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Flat flat = flatsCache.getFlatAtLocation(event.getBlock().getLocation());
        cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() != null) {
            Flat flat = flatsCache.getFlatAtLocation(event.getClickedBlock().getLocation());
            cancelEventIfPlayerNotTrustedOrOwner(event, flat, player);
            return;
        }
        if (event.getInteractionPoint() != null) {
            Flat flat = flatsCache.getFlatAtLocation(event.getInteractionPoint());
            cancelEventIfPlayerNotTrustedOrOwner(event, flat, player);
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Flat flat = flatsCache.getFlatAtLocation(event.getEntity().getLocation());
        cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getRemover());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Flat flat = flatsCache.getFlatAtLocation(block.getLocation());
            return flat != null;
        });
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Flat flat = flatsCache.getFlatAtLocation(event.getEntity().getLocation());
        cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getDamager());
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Flat flat = flatsCache.getFlatAtLocation(event.getBlock().getLocation());
        cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getEntity());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Flat flat = flatsCache.getFlatAtLocation(event.getRightClicked().getLocation());
        cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getPlayer());
    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Flat flat = flatsCache.getFlatAtLocation(event.getBlock().getLocation());
        cancelEventIfPlayerNotTrustedOrOwner(event, flat, event.getPlayer());
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Flat toFlat = flatsCache.getFlatAtLocation(event.getToBlock().getLocation());
        Flat fromFlat = flatsCache.getFlatAtLocation(event.getBlock().getLocation());

        if (toFlat != null && !toFlat.equals(fromFlat)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (flatsCache.getFlatAtLocation(event.getBlock().getLocation()) != null) {
            return;
        }
        event.blockList().removeIf(block -> {
            Flat flat = flatsCache.getFlatAtLocation(block.getLocation());
            return flat != null;
        });
    }

}
