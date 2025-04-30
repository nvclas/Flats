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
    private final List<OfflinePlayer> trusted;
    private String name;
    private OfflinePlayer owner;

    public Flat(String name, Area area) {
        this.name = name;
        areas = new ArrayList<>(List.of(area));
        trusted = new ArrayList<>();
    }

    public Flat(String name, OfflinePlayer owner, List<Area> areas, List<OfflinePlayer> trusted) {
        this.name = name;
        this.owner = owner;
        this.areas = areas;
        this.trusted = trusted;
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
        for (Area area : areas) {
            if (area.isWithinBounds(location)) {
                return true;
            }
        }
        return false;
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
     * Determines whether this {@link Flat} has an owner assigned.
     *
     * @return {@code true} if an owner is assigned to this flat; {@code false} otherwise.
     */
    public boolean hasOwner() {
        return owner != null;
    }

    /**
     * Checks if the given {@link OfflinePlayer} is in the list of trusted players for this flat.
     *
     * @param player The {@link OfflinePlayer} to check. Must not be null.
     * @return {@code true} if the specified player is trusted; {@code false} otherwise.
     */
    public boolean isTrusted(@NotNull OfflinePlayer player) {
        return trusted.contains(player);
    }

    /**
     * Adds the specified {@link OfflinePlayer} to the list of trusted players for this flat.
     * <p>
     * Trusted players have access to this flat's resources.
     *
     * @param player The {@link OfflinePlayer} to be added to the trusted list. Must not be null.
     */
    public void addTrusted(@NotNull OfflinePlayer player) {
        trusted.add(player);
    }

    /**
     * Removes the specified {@link OfflinePlayer} from the list of trusted players for this flat.
     *
     * @param player The {@link OfflinePlayer} to be removed from the trusted list. Must not be null.
     */
    public void removeTrusted(@NotNull OfflinePlayer player) {
        trusted.remove(player);
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
