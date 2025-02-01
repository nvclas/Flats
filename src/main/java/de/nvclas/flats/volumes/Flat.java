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

    public boolean isWithinBounds(@NotNull Location location) {
        return areas.stream().anyMatch(area -> area.isWithinBounds(location));
    }

    public boolean isOwner(@NotNull OfflinePlayer player) {
        return owner != null && owner.getUniqueId().equals(player.getUniqueId());
    }

    public void addArea(@NotNull Area area) {
        areas.add(area);
    }

}
