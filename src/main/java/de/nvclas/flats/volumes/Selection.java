package de.nvclas.flats.volumes;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Represents a three-dimensional selection defined by two corner points.
 * A selection can calculate its volume, check for intersections with other areas,
 * and is associated with specific players through a global map.
 */
@Getter
@Setter
public class Selection {

    private static final Map<Player, Selection> selections = new WeakHashMap<>();
    private Location pos1;
    private Location pos2;

    /**
     * Retrieves the {@link Selection} associated with the given {@link Player}.
     * If no selection exists for the specified player, a new one is created and returned.
     *
     * @param player The player whose selection is to be retrieved. Must not be null.
     * @return The {@link Selection} object associated with the given player. Never null.
     */
    public static Selection getSelection(Player player) {
        return selections.computeIfAbsent(player, k -> new Selection());
    }

    /**
     * Calculates the volume of a three-dimensional cuboid defined by two corner points {@code pos1} and {@code pos2}.
     * <p>
     * If either {@code pos1} or {@code pos2} is {@code null}, the method returns {@code 0}.
     *
     * @return The calculated volume as an {@code int}, or {@code 0} if the positions are not defined.
     */
    public int calculateVolume() {
        if (pos1 == null || pos2 == null) {
            return 0;
        }
        int length = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
        int height = Math.abs(pos2.getBlockY() - pos1.getBlockY()) + 1;
        int width = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;

        return length * height * width;
    }

    /**
     * Removes the current {@link Selection} instance from the global selection map.
     * <p>
     * This method effectively clears the selection associated with this object.
     */
    public void clear() {
        selections.values().remove(this);
    }

    /**
     * Checks if the current selection intersects with the specified {@link Area}.
     *
     * @param area the {@code Area} to test for intersection with the current selection
     * @return {@code true} if the two areas intersect, {@code false} otherwise
     */
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
