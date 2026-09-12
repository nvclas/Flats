package de.nvclas.flats.listeners;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Selection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class StickInteractListener implements Listener {

    private final Flats flatsPlugin;
    private final SettingsConfig settingsConfig;

    public StickInteractListener(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
        this.settingsConfig = flatsPlugin.getSettingsConfig();
    }

    @EventHandler
    public void onStickInteraction(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getItem() == null || !event.getItem().isSimilar(SelectionItem.getItem(flatsPlugin))
                || !Permissions.canEditFlats(player, settingsConfig)) {
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
            player.sendMessage(Flats.PREFIX + I18n.translate("selection.set", "1", selection.calculateVolume()));
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selection.setPos2(clickedLocation);
            player.sendMessage(Flats.PREFIX + I18n.translate("selection.set", "2", selection.calculateVolume()));
        }
    }
}
