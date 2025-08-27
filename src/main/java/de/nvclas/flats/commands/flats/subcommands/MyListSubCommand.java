package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.CommandUtils;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MyListSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public MyListSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (CommandUtils.isCommandOnCooldown(player, "flats mylist")) {
            return;
        }

        if (!Permissions.canListFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }

        List<Flat> playerFlats = flatsCache.getAllFlats().stream()
            .filter(flat -> flat.getOwner() != null && player.getUniqueId().equals(flat.getOwner().getUniqueId()))
            .toList();

        if (playerFlats.isEmpty()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("mylist.empty"));
            return;
        }

        player.sendMessage(Flats.PREFIX + I18n.translate("mylist.title"));
        
        for (Flat flat : playerFlats) {
            String flatName = flat.getName();
            List<Area> areas = flat.getAreas();
            
            // Send flat name
            player.sendMessage("§6§l" + flatName);
            
            // List trusted players if any
            if (!flat.getTrusted().isEmpty()) {
                player.sendMessage("  §7├§6Trusted: §e" + flat.getTrusted().size() + " player(s)");
            }
            
            // List areas
            if (areas.size() == 1) {
                player.sendMessage("  §7└§6Area: §e" + areas.get(0).getLocationString());
            } else {
                player.sendMessage("  §7├§6Areas: §e" + areas.size());
                for (int i = 0; i < areas.size(); i++) {
                    if (i == areas.size() - 1) {
                        player.sendMessage("    §7└§e" + areas.get(i).getLocationString());
                    } else {
                        player.sendMessage("    §7├§e" + areas.get(i).getLocationString());
                    }
                }
            }
        }
        
        player.sendMessage(Flats.PREFIX + I18n.translate("mylist.total", playerFlats.size()));
    }
}