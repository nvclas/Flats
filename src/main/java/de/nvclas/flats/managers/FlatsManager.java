package de.nvclas.flats.managers;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@UtilityClass
public class FlatsManager {

    private final Map<String, Flat> allFlats = new HashMap<>();
    private final FlatsConfig config = Flats.getInstance().getFlatsConfig();

    public void initialize() {
        allFlats.clear();
        allFlats.putAll(config.loadFlats());
    }

    public void shutdown() {
        config.saveFlats(allFlats);
    }

    public @NotNull List<String> getAllFlatNames() {
        return List.copyOf(allFlats.keySet());
    }

    public @NotNull List<Flat> getAllFlats() {
        return List.copyOf(allFlats.values());
    }

    public @NotNull List<Area> getAllAreas() {
        return allFlats.values().stream().flatMap(flat -> flat.getAreas().stream()).toList();
    }
    
    public Flat getFlat(@NotNull String name) {
        if (!existisFlat(name)) {
            return null;
        }
        return Objects.requireNonNull(allFlats.get(name), "Oops, something went terribly wrong. Please restart the server!");
    }
    
    public @Nullable Flat getFlatByLocation(@NotNull Location location) {
        return allFlats.values()
                .stream()
                .filter(flat -> flat.isWithinBounds(location))
                .findFirst()
                .orElse(null);
    }
    
    public void setOwner(Flat flat, OfflinePlayer owner) {
        flat.setOwner(owner);
    }

    public void addArea(@NotNull String name, @NotNull Area area) throws IllegalArgumentException {
        if (!existisFlat(name)) {
            throw new IllegalArgumentException("No flat exists with the given name: " + name);
        }
        Flat flat = getFlat(name);
        flat.addArea(area);
    }
    
    public void create(@NotNull String name, @NotNull Area area) throws IllegalArgumentException {
        if (existisFlat(name)) {
            throw new IllegalArgumentException("A flat with this name already exists.");
        }
        Flat newFlat = new Flat(name, area);
        allFlats.put(name, newFlat);
    }
    
    public void delete(@NotNull String name) throws IllegalArgumentException {
        if (!existisFlat(name)) {
            throw new IllegalArgumentException("No flat exists with the given name: " + name);
        }
        allFlats.remove(name);
    }

    public boolean existisFlat(@NotNull String name) {
        return allFlats.containsKey(name);
    }

}