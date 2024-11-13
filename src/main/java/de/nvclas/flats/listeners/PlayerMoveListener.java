package de.nvclas.flats.listeners;

import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.selection.Flat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PlayerMoveListener implements Listener {
    private final FlatsConfig flatsConfig;
    private final Map<Player, Flat> playerFlats = new HashMap<>();

    public PlayerMoveListener(FlatsConfig flatsConfig) {
        this.flatsConfig = flatsConfig;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Flat currentFlat = playerFlats.get(player);
        Flat newFlat = flatsConfig.getFlatByLocation(event.getTo());

        if (!Objects.equals(currentFlat, newFlat)) {
            if (currentFlat != null) {
                // Player left the current flat
                player.sendMessage("You left the flat.");
            }
            if (newFlat != null) {
                // Player entered a new flat
                player.sendMessage("You entered a new flat.");
            }
        }
        playerFlats.put(player, newFlat);
    }
}