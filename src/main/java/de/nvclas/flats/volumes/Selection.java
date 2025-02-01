package de.nvclas.flats.volumes;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.WeakHashMap;

@Getter
@Setter
public class Selection {

    private static final Map<Player, Selection> selections = new WeakHashMap<>();
    private Location pos1;
    private Location pos2;

    public static Selection getSelection(Player player) {
        if (!selections.containsKey(player)) {
            selections.put(player, new Selection());
        }
        return selections.get(player);
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

    public void clear() {
        selections.values().remove(this);
    }

    public boolean intersects(Area area) {
        double maxX1 = Math.max(pos1.getX(), pos2.getX());
        double minX1 = Math.min(pos1.getX(), pos2.getX());
        double maxY1 = Math.max(pos1.getY(), pos2.getY());
        double minY1 = Math.min(pos1.getY(), pos2.getY());
        double maxZ1 = Math.max(pos1.getZ(), pos2.getZ());
        double minZ1 = Math.min(pos1.getZ(), pos2.getZ());

        double maxX2 = Math.max(area.getPos1().getX(), area.getPos2().getX());
        double minX2 = Math.min(area.getPos1().getX(), area.getPos2().getX());
        double maxY2 = Math.max(area.getPos1().getY(), area.getPos2().getY());
        double minY2 = Math.min(area.getPos1().getY(), area.getPos2().getY());
        double maxZ2 = Math.max(area.getPos1().getZ(), area.getPos2().getZ());
        double minZ2 = Math.min(area.getPos1().getZ(), area.getPos2().getZ());

        boolean intersectsX = minX1 <= maxX2 && maxX1 >= minX2;
        boolean intersectsY = minY1 <= maxY2 && maxY1 >= minY2;
        boolean intersectsZ = minZ1 <= maxZ2 && maxZ1 >= minZ2;

        return intersectsX && intersectsY && intersectsZ;
    }
}
