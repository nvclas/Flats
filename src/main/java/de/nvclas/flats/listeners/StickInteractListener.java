package de.nvclas.flats.listeners;

import de.nvclas.flats.Flats;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Selection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class StickInteractListener implements Listener {

    @EventHandler
    public void onStickInteraction(@NotNull PlayerInteractEvent event) {
        Player p = event.getPlayer();

        if (event.getItem() == null) return;
        if (!event.getItem().isSimilar(SelectionItem.getItem())) return;
        if (!p.hasPermission(Permissions.ADMIN)) return;

        event.setCancelled(true);
        Selection selection = Selection.getSelection(p);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            selection.setPos1(event.getClickedBlock().getLocation());
            p.sendMessage(Flats.PREFIX + I18n.translate("selection.set", "1", selection.calculateVolume()));
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            selection.setPos2(event.getClickedBlock().getLocation());
            p.sendMessage(Flats.PREFIX + I18n.translate("selection.set", "2", selection.calculateVolume()));
        }
    }

}
