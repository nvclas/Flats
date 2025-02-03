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

    /**
     * Retrieves the {@link Selection} associated with the specified player.
     * <p>
     * If no selection exists for the player, a new selection is created, associated with the player,
     * and then returned. Uses a weak reference to ensure unused selections can be garbage collected.
     *
     * @param player The {@link Player} for whom the selection is retrieved or created. Must not be null.
     * @return The {@link Selection} object associated with the given player.
     */
    public static Selection getSelection(Player player) {
        return selections.computeIfAbsent(player, k -> new Selection());
    }

    /**
     * Calculates the volume of the selection defined by the positions {@code pos1} and {@code pos2}.
     * <p>
     * The volume is determined by treating the selection as a rectangular prism, where {@code pos1} and {@code pos2}
     * represent two opposite corners of the prism. If either position is {@code null}, the volume is defined as {@code 0}.
     *
     * @return The calculated volume of the selection as an integer. Returns {@code 0} if either {@code pos1} or {@code pos2} is {@code null}.
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
     * Clears this selection by removing it from the collection of selections
     * managed by the containing class.
     * <p>
     * This method ensures that the current {@link Selection} instance is
     * removed from the {@code selections} map, effectively disassociating
     * it from any tracked entries.
     */
    public void clear() {
        selections.values().remove(this);
    }

    /**
     * Determines whether this selection intersects with the provided {@link Area}.
     * <p>
     * This method checks if the volumetric boundaries of the current selection
     * overlap with those of the given area in three-dimensional space.
     *
     * @param area The {@link Area} to check for intersection. Must not be null.
     * @return {@code true} if this selection intersects with the specified area;
     * {@code false} otherwise.
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
