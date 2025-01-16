package de.nvclas.flats.listeners;

import de.nvclas.flats.events.FlatEnteredOrLeftEvent;
import de.nvclas.flats.selection.Flat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PlayerMoveListener implements Listener {
    private final Map<Player, Flat> playerFlats = new HashMap<>();

    @EventHandler
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Flat currentFlat = playerFlats.get(player);
        Flat newFlat = Flat.getFlatByLocation(event.getTo());

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