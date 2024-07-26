package de.nvclas.flats.selection;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
public class Selection {

    private static final Map<Player, Selection> selections = new HashMap<>();

    private Location pos1;
    private Location pos2;

    public Selection() {
    }

    public Selection(Location pos1, Location pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public static Selection getSelection(Player p) {
        selections.putIfAbsent(p, new Selection());
        return selections.get(p);
    }

    public int calculateVolume() {
        if (pos1 == null || pos2 == null) {
            return 0;
        }

        int length = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
        int height = Math.abs(pos2.getBlockY() - pos1.getBlockY()) + 1;
        int width = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;

        return length * height * width;
    }

    public boolean intersects(Selection other) {
        double maxX1 = Math.max(pos1.getX(), pos2.getX());
        double minX1 = Math.min(pos1.getX(), pos2.getX());
        double maxY1 = Math.max(pos1.getY(), pos2.getY());
        double minY1 = Math.min(pos1.getY(), pos2.getY());
        double maxZ1 = Math.max(pos1.getZ(), pos2.getZ());
        double minZ1 = Math.min(pos1.getZ(), pos2.getZ());

        double maxX2 = Math.max(other.pos1.getX(), other.pos2.getX());
        double minX2 = Math.min(other.pos1.getX(), other.pos2.getX());
        double maxY2 = Math.max(other.pos1.getY(), other.pos2.getY());
        double minY2 = Math.min(other.pos1.getY(), other.pos2.getY());
        double maxZ2 = Math.max(other.pos1.getZ(), other.pos2.getZ());
        double minZ2 = Math.min(other.pos1.getZ(), other.pos2.getZ());

        boolean intersectsX = minX1 <= maxX2 && maxX1 >= minX2;
        boolean intersectsY = minY1 <= maxY2 && maxY1 >= minY2;
        boolean intersectsZ = minZ1 <= maxZ2 && maxZ1 >= minZ2;

        return intersectsX && intersectsY && intersectsZ;
    }

    public boolean intersects(Location location) {
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        double minX = Math.min(pos1.getX(), pos2.getX());

        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minY = Math.min(pos1.getY(), pos2.getY());

        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
        double minZ = Math.min(pos1.getZ(), pos2.getZ());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public Set<Block> getBlockList() {
        Set<Block> blocks = new HashSet<>();
        World world = pos1.getWorld();

        if (world == null) {
            return blocks;
        }
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());

        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());

        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    public void clear() {
        pos1 = null;
        pos2 = null;
    }
    
}
