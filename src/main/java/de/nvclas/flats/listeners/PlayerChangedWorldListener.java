package de.nvclas.flats.listeners;

import de.nvclas.flats.selection.Selection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerChangedWorldListener implements Listener {
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        Selection.getSelection(p).clear();
    }
}
