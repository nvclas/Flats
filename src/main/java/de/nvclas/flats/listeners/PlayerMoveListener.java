package de.nvclas.flats.listeners;

import de.nvclas.flats.Flats;
import de.nvclas.flats.events.FlatEnteredOrLeftEvent;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class PlayerMoveListener implements Listener {
    private final Flats flatsPlugin;
    private final Map<Player, Flat> playerFlats = new WeakHashMap<>();

    public PlayerMoveListener(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }

    @EventHandler
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Flat currentFlat = playerFlats.get(player);
        Flat newFlat = flatsPlugin.getFlatsManager().getFlatByLocation(event.getTo());

        if (!Objects.equals(currentFlat, newFlat)) {
            if (currentFlat != null) {
                new FlatEnteredOrLeftEvent(currentFlat, player, false).callEvent();
            }
            if (newFlat != null) {
                new FlatEnteredOrLeftEvent(newFlat, player, true).callEvent();
            }
            playerFlats.put(player, newFlat);
        }
    }
}