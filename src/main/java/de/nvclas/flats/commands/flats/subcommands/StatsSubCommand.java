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

public class StatsSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public StatsSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (CommandUtils.isCommandOnCooldown(player, "flats stats")) {
            return;
        }

        if (!Permissions.canInfoFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }

        if (args.length < 1) {
            player.sendMessage(Flats.PREFIX + I18n.translate("stats.usage"));
            return;
        }

        String flatName = args[0];

        if (!flatsCache.existsFlat(flatName)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_not_exist"));
            return;
        }

        Flat flat = flatsCache.getExistingFlat(flatName);
        
        // Check if player has permission to view stats (owner, trusted, or admin)
        boolean canViewStats = flat.getOwner() != null && player.getUniqueId().equals(flat.getOwner().getUniqueId()) ||
                              flat.getTrusted().contains(player) ||
                              Permissions.hasAdminPermission(player);
        
        if (!canViewStats) {
            player.sendMessage(Flats.PREFIX + I18n.translate("stats.no_permission"));
            return;
        }

        displayFlatStatistics(player, flat);
    }

    private void displayFlatStatistics(Player player, Flat flat) {
        player.sendMessage(Flats.PREFIX + I18n.translate("stats.header", flat.getName()));
        
        // Basic information
        String ownerName = flat.getOwner() != null ? flat.getOwner().getName() : I18n.translate("info.unoccupied");
        player.sendMessage("  §7├§6Owner: §e" + ownerName);
        
        // Trusted players count
        int trustedCount = flat.getTrusted().size();
        player.sendMessage("  §7├§6Trusted Players: §e" + trustedCount);
        
        // Areas information
        List<Area> areas = flat.getAreas();
        player.sendMessage("  §7├§6Areas: §e" + areas.size());
        
        // Calculate total volume
        int totalVolume = 0;
        for (Area area : areas) {
            totalVolume += area.calculateVolume();
        }
        player.sendMessage("  §7├§6Total Volume: §e" + totalVolume + " blocks");
        
        // Calculate percentage of max flat size used
        int maxFlatSize = settingsConfig.getMaxFlatSize();
        double percentageUsed = (double) totalVolume / maxFlatSize * 100;
        player.sendMessage("  §7├§6Size Usage: §e" + String.format("%.1f", percentageUsed) + "% §7of maximum");
        
        // Area details
        if (areas.size() > 1) {
            player.sendMessage("  §7├§6Area Details:");
            for (int i = 0; i < areas.size(); i++) {
                Area area = areas.get(i);
                int volume = area.calculateVolume();
                String prefix = (i == areas.size() - 1) ? "    §7└" : "    §7├";
                player.sendMessage(prefix + "§eArea " + (i + 1) + ": §f" + volume + " blocks §7at " + area.getLocationString());
            }
        } else if (areas.size() == 1) {
            Area area = areas.get(0);
            player.sendMessage("  §7├§6Location: §e" + area.getLocationString());
        }
        
        // World information
        if (!areas.isEmpty()) {
            String worldName = areas.get(0).getWorld().getName();
            player.sendMessage("  §7└§6World: §e" + worldName);
        }
    }
}