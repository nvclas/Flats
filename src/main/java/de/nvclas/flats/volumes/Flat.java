package de.nvclas.flats.volumes;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Flat {

    private final List<Area> areas;
    private OfflinePlayer owner;
    private String name;

    public Flat(String name, Area area) {
        this.name = name;
        areas = new ArrayList<>();
        areas.add(area);
    }

    public Flat(String name, List<Area> areas, OfflinePlayer owner) {
        this.name = name;
        this.areas = areas;
        this.owner = owner;
    }

    /**
     * Checks if the specified {@link Location} is within the bounds of any {@link Area}
     * in the list of areas associated with the flat.
     * <p>
     * This method iterates through all areas and uses {@link Area#isWithinBounds(Location)}
     * to verify if the location is contained within the bounds of any defined area.
     *
     * @param location The {@link Location} to check. Must not be null.
     * @return {@code true} if the {@code location} is within the bounds of at least one area;
     * {@code false} otherwise.
     */
    public boolean isWithinBounds(@NotNull Location location) {
        return areas.stream().anyMatch(area -> area.isWithinBounds(location));
    }

    /**
     * Checks if the specified {@link OfflinePlayer} is the owner of this flat.
     * <p>
     * The method compares the unique identifier of the given player with the unique identifier
     * of the owner of the flat to determine ownership.
     *
     * @param player The {@link OfflinePlayer} to check. Must not be null.
     * @return {@code true} if the specified player is the owner of the flat;
     * {@code false} otherwise.
     */
    public boolean isOwner(@NotNull OfflinePlayer player) {
        return owner != null && owner.getUniqueId().equals(player.getUniqueId());
    }

    /**
     * Adds a new {@link Area} to the list of areas associated with the flat.
     * <p>
     * This method appends the specified {@link Area} instance to the internal list of areas
     * for this flat. The data structure ensures all defined areas are managed
     * within the flat's context.
     *
     * @param area The {@link Area} to be added to the flat. Must not be null.
     */
    public void addArea(@NotNull Area area) {
        areas.add(area);
    }

}
