package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.schedulers.CommandDelayScheduler;
import de.nvclas.flats.util.CommandUtils;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeleportSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public TeleportSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (CommandUtils.isCommandOnCooldown(player, "flats tp")) {
            return;
        }

        if (!Permissions.canClaimFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("teleport.usage"));
            return;
        }

        String flatName = args[1];

        if (!flatsCache.existsFlat(flatName)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_not_exist"));
            return;
        }

        Flat flat = flatsCache.getExistingFlat(flatName);
        if (flat.getOwner() == null || !player.getUniqueId().equals(flat.getOwner().getUniqueId())) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.not_your_flat"));
            return;
        }

        // Add teleport cooldown delay using the settings config
        CommandDelayScheduler.addDelay(player, "flats tp", settingsConfig.getTeleportCooldown());

        // Find the center of the first area and teleport there
        Area firstArea = flat.getAreas().get(0);
        Location teleportLocation = calculateSafeTeleportLocation(firstArea);
        
        player.teleport(teleportLocation);
        player.sendMessage(Flats.PREFIX + I18n.translate("teleport.success", flatName));
    }

    /**
     * Calculates a safe teleport location within the given area.
     * Returns the center of the area at a safe height.
     */
    private Location calculateSafeTeleportLocation(Area area) {
        // Get the center coordinates of the area
        double centerX = (area.getMinX() + area.getMaxX()) / 2.0;
        double centerZ = (area.getMinZ() + area.getMaxZ()) / 2.0;
        
        // Use the middle Y coordinate as a starting point, but ensure it's safe
        double centerY = (area.getMinY() + area.getMaxY()) / 2.0;
        
        // Create the teleport location
        Location location = new Location(area.getWorld(), centerX, centerY, centerZ);
        
        // Adjust Y to find a safe spot (simple implementation)
        // In a more sophisticated version, we could check for solid blocks and air space
        location.setY(Math.max(centerY, area.getWorld().getHighestBlockYAt(location) + 1));
        
        return location;
    }
}