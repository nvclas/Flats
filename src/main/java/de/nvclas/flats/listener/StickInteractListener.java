package de.nvclas.flats.listener;

import de.nvclas.flats.Flats;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.selection.Selection;
import de.nvclas.flats.utils.Permissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class StickInteractListener implements Listener {

    @EventHandler
    public void onStickIneraction(PlayerInteractEvent event) {
        Player p = event.getPlayer();

        if (event.getItem() == null) return;
        if (!event.getItem().isSimilar(SelectionItem.getItem())) return;
        if (!p.hasPermission(Permissions.ADMIN)) return;

        event.setCancelled(true);
        Selection selection = Selection.getSelection(p);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            selection.setPos1(event.getClickedBlock().getLocation());
            p.sendMessage(Flats.PREFIX + "§aPosition 1 gesetzt (" + selection.calculateVolume() + ")");
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            selection.setPos2(event.getClickedBlock().getLocation());
            p.sendMessage(Flats.PREFIX + "§aPosition 2 gesetzt (" + selection.calculateVolume() + ")");
        }
    }

}
