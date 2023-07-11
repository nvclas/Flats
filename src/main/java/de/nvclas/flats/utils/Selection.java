package de.nvclas.flats.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

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
        if (!selections.containsKey(p)) selections.put(p, new Selection());
        return selections.get(p);
    }

    public int calculateVolume() {
        if (pos1 == null || pos2 == null) return 0;

        int length = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
        int height = Math.abs(pos2.getBlockY() - pos1.getBlockY()) + 1;
        int width = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;

        return length * height * width;
    }

    public boolean intersects(Selection other) {
        double minX1 = Math.min(pos1.getX(), pos2.getX());
        double minY1 = Math.min(pos1.getY(), pos2.getY());
        double minZ1 = Math.min(pos1.getZ(), pos2.getZ());
        double maxX1 = Math.max(pos1.getX(), pos2.getX());
        double maxY1 = Math.max(pos1.getY(), pos2.getY());
        double maxZ1 = Math.max(pos1.getZ(), pos2.getZ());

        double minX2 = Math.min(other.pos1.getX(), other.pos2.getX());
        double minY2 = Math.min(other.pos1.getY(), other.pos2.getY());
        double minZ2 = Math.min(other.pos1.getZ(), other.pos2.getZ());
        double maxX2 = Math.max(other.pos1.getX(), other.pos2.getX());
        double maxY2 = Math.max(other.pos1.getY(), other.pos2.getY());
        double maxZ2 = Math.max(other.pos1.getZ(), other.pos2.getZ());

        boolean intersectsX = minX1 <= maxX2 && maxX1 >= minX2;
        boolean intersectsY = minY1 <= maxY2 && maxY1 >= minY2;
        boolean intersectsZ = minZ1 <= maxZ2 && maxZ1 >= minZ2;

        return intersectsX && intersectsY && intersectsZ;
    }

    public boolean intersects(Location location) {
        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }
}
