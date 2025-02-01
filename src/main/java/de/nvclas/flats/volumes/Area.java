package de.nvclas.flats.volumes;

import de.nvclas.flats.utils.LocationConverter;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Area {

    private final Location pos1;
    private final Location pos2;
    private final String flatName;
    private final String locationString;

    public Area(Location pos1, Location pos2, String flatName) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.flatName = flatName;
        this.locationString = LocationConverter.getStringFromLocations(pos1, pos2);
    }

    public static Area fromString(@NotNull String locationString, @NotNull String flatName) {
        Location[] locations = LocationConverter.getLocationsFromString(locationString);
        return new Area(locations[0], locations[1], flatName);
    }

    public static Area fromSelection(@NotNull Selection selection, @NotNull String flatName) {
        return new Area(selection.getPos1(), selection.getPos2(), flatName);
    }

    public boolean isWithinBounds(@NotNull Location location) {
        double minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        double maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        double minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        double maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        double minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        double maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return location.getBlockX() >= minX && location.getBlockX() <= maxX && location.getBlockY() >= minY && location.getBlockY() <= maxY && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public boolean isWithinDistance(@NotNull Location playerLocation, double range) {
        return (Math.abs(playerLocation.getX() - pos1.getX()) <= range && Math.abs(playerLocation.getY() - pos1.getY()) <= range && Math.abs(
                playerLocation.getZ() - pos1.getZ()) <= range) || (Math.abs(playerLocation.getX() - pos2.getX()) <= range && Math.abs(
                playerLocation.getY() - pos2.getY()) <= range && Math.abs(playerLocation.getZ() - pos2.getZ()) <= range);
    }

    public @NotNull List<Block> getAllOuterBlocks() {
        List<Block> blocks = new ArrayList<>();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                        blocks.add(pos1.getWorld().getBlockAt(x, y, z));
                    }
                }
            }
        }
        return blocks;
    }

}
