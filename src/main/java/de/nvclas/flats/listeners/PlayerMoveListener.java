package de.nvclas.flats.listeners;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.cache.SpatialIndex;
import de.nvclas.flats.events.FlatEnteredOrLeftEvent;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class PlayerMoveListener implements Listener {

    private final FlatsCache flatsCache;

    private final Map<Player, Flat> playerFlats = new WeakHashMap<>();

    public PlayerMoveListener(Flats flatsPlugin) {
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Location location = event.getPlayer().getLocation();
        flatsCache.prefetchSurroundingGridCells(location, 1);
        updatePlayerFlat(event.getPlayer(), location);
    }

    @EventHandler
    public void onPlayerTeleport(@NotNull PlayerTeleportEvent event) {
        Location to = event.getTo();
        flatsCache.prefetchSurroundingGridCells(to, 1);
        updatePlayerFlat(event.getPlayer(), to);
    }

    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        Location respawnLocation = event.getRespawnLocation();
        flatsCache.prefetchSurroundingGridCells(respawnLocation, 1);
        updatePlayerFlat(event.getPlayer(), respawnLocation);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        playerFlats.remove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        int fromGridX = Math.floorDiv(from.getBlockX(), SpatialIndex.GRID_SIZE);
        int fromGridZ = Math.floorDiv(from.getBlockZ(), SpatialIndex.GRID_SIZE);
        int toGridX = Math.floorDiv(to.getBlockX(), SpatialIndex.GRID_SIZE);
        int toGridZ = Math.floorDiv(to.getBlockZ(), SpatialIndex.GRID_SIZE);

        if (fromGridX != toGridX || fromGridZ != toGridZ || !Objects.equals(from.getWorld(), to.getWorld())) {
            flatsCache.prefetchSurroundingGridCells(to, 1);
        }

        updatePlayerFlat(event.getPlayer(), to);
    }

    private void updatePlayerFlat(@NotNull Player player, @NotNull Location location) {
        if (!flatsCache.isLocationLoaded(location)) {
            return;
        }

        Flat currentFlat = playerFlats.get(player);
        Flat newFlat = flatsCache.getFlatAtLocation(location);

        if (Objects.equals(currentFlat, newFlat)) {
            return;
        }

        if (currentFlat != null) {
            new FlatEnteredOrLeftEvent(currentFlat, player, false).callEvent();
        }

        if (newFlat != null) {
            new FlatEnteredOrLeftEvent(newFlat, player, true).callEvent();
            playerFlats.put(player, newFlat);
            return;
        }

        playerFlats.remove(player);
    }
}
