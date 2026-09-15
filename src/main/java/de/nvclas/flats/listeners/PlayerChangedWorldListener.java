package de.nvclas.flats.listeners;

import de.nvclas.flats.volumes.Selection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerChangedWorldListener implements Listener {
    @EventHandler
    public void onPlayerChangedWorld(@NotNull PlayerChangedWorldEvent event) {
        Selection.getSelection(event.getPlayer()).clear();
    }
}
