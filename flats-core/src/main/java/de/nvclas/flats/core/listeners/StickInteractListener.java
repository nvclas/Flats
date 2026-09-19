package de.nvclas.flats.core.listeners;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.items.SelectionItem;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permissions;
import de.nvclas.flats.core.volumes.Selection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class StickInteractListener implements Listener {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;

    public StickInteractListener(BaseFlats plugin) {
        this.plugin = plugin;
        this.configAdapter = plugin.getConfigAdapter();
    }

    @EventHandler
    public void onStickInteraction(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getItem() == null || !event.getItem().isSimilar(SelectionItem.getItem(plugin))
                || !Permissions.canEditFlats(plugin, player, configAdapter)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedBlock() == null) {
            return;
        }

        Selection selection = Selection.getSelection(player);
        Location clickedLocation = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selection.setPos1(clickedLocation);
            player.sendMessage(plugin.getPrefix() + I18n.translate("selection.set", "1", selection.calculateVolume()));
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selection.setPos2(clickedLocation);
            player.sendMessage(plugin.getPrefix() + I18n.translate("selection.set", "2", selection.calculateVolume()));
        }
    }
}
