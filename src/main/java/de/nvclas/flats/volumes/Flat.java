package de.nvclas.flats.volumes;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a flat composed of multiple {@link Area} objects.
 * A flat can have an owner and a unique name.
 */
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
     * Checks whether the given {@link Location} is within the bounds of any {@link Area}
     * in the current flat.
     *
     * @param location The {@link Location} to check. Must not be null.
     * @return {@code true} if the {@code location} is within the bounds of at least one {@link Area};
     * {@code false} otherwise.
     */
    public boolean isWithinBounds(@NotNull Location location) {
        return areas.stream().anyMatch(area -> area.isWithinBounds(location));
    }

    /**
     * Checks whether the specified {@link OfflinePlayer} is the owner of this {@code Flat}.
     *
     * @param player The {@link OfflinePlayer} to check. Must not be null.
     * @return {@code true} if the given player is the owner of this flat; {@code false} otherwise.
     */
    public boolean isOwner(@NotNull OfflinePlayer player) {
        return owner != null && owner.getUniqueId().equals(player.getUniqueId());
    }

    /**
     * Adds a new {@link Area} to the list of areas associated with this flat.
     *
     * @param area The {@link Area} to add. Must not be null.
     */
    public void addArea(@NotNull Area area) {
        areas.add(area);
    }

}
