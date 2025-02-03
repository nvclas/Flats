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

/**
 * Utility class for managing {@link Flat} instances and related operations in the application.
 * <p>
 * This class provides functionality for managing flats, including loading and saving data,
 * retrieving flat details, and performing operations such as creating, deleting,
 * or modifying flats. It acts as a central hub for managing flats and their associated
 * data using the {@link FlatsConfig} for persistence.
 * <p>
 * This class is a singleton and should not be instantiated.
 */
@UtilityClass
public class FlatsManager {

    private final Map<String, Flat> allFlats = new HashMap<>();
    private final FlatsConfig config = Flats.getInstance().getFlatsConfig();

    /**
     * Loads all flats from the configuration and populates the internal storage with the retrieved data.
     * <p>
     * This method is responsible for clearing the current state of all loaded flats by
     * removing any existing entries from the internal map and replacing them with the data
     * retrieved from the {@link FlatsConfig}. The underlying configuration source
     * is accessed to fetch the list of flats, which is then fully loaded into the internal map.
     * <p>
     * This operation ensures the internal state of all flats is consistent and synchronized
     * with the persisted configuration.
     * <p>
     * Delegates the loading of flats to {@link FlatsConfig#loadFlats()}.
     */
    public void loadAll() {
        allFlats.clear();
        allFlats.putAll(config.loadFlats());
    }

    /**
     * Saves all flats to the configuration.
     * <p>
     * This method delegates the operation to {@link FlatsConfig#saveFlats(Map)}, which resets the current
     * flats section in the configuration and serializes the state of all flats stored in the internal
     * {@code allFlats} map. The configuration is updated and saved to persist any changes made during
     * the runtime of the application.
     * <p>
     * This method ensures that the internal state of flats managed by {@code FlatsManager} is properly
     * synchronized with the configuration file when invoked, typically during application shutdown or
     * periodic save operations.
     */
    public void saveAll() {
        config.saveFlats(allFlats);
    }

    /**
     * Retrieves a list of all flat names currently managed.
     * <p>
     * The names returned by this method represent keys of the internal map
     * containing flat definitions. The list is immutable to ensure thread safety,
     * and any modifications to the underlying data structures will not affect the
     * returned list.
     *
     * @return an immutable {@link List} of {@link String}, each representing the name of a flat.
     */
    public @NotNull List<String> getAllFlatNames() {
        return List.copyOf(allFlats.keySet());
    }

    /**
     * Retrieves a list of all flats currently managed by the system.
     * <p>
     * This method accesses the internal storage of all loaded flats and provides
     * a read-only view of all {@link Flat} instances managed by the {@code FlatsManager}.
     *
     * @return An immutable {@link List} containing all {@link Flat} objects currently stored.
     * The list will be empty if no flats are present.
     */
    public @NotNull List<Flat> getAllFlats() {
        return List.copyOf(allFlats.values());
    }

    /**
     * Retrieves a list of all {@link Area} instances across all flats.
     * <p>
     * This method aggregates the areas of all flats stored in the internal
     * {@link #allFlats} map. It traverses the map's values, which represent
     * individual flats, collects their areas via {@link Flat#getAreas()}, and
     * returns a flattened list of all areas.
     *
     * @return A {@link List} of all {@link Area} objects defined within the flats. The returned list is non-null but may be empty if no areas are defined.
     */
    public @NotNull List<Area> getAllAreas() {
        return allFlats.values().stream().flatMap(flat -> flat.getAreas().stream()).toList();
    }

    /**
     * Retrieves a {@link Flat} object associated with the specified name.
     * <p>
     * This method checks if a flat with the given {@code name} exists using {@link #existsFlat(String)}.
     * If the flat does not exist, the method returns {@code null}.
     * Otherwise, it retrieves the flat from the internal storage map.
     * Additional precautions are taken to ensure a non-null value is returned from the internal map;
     * an exception is thrown in case of unexpected inconsistencies.
     *
     * @param name The name of the flat to retrieve. Must not be null.
     * @return The {@link Flat} object associated with the specified name, or {@code null} if no such flat exists.
     * @throws NullPointerException If an unexpected inconsistency occurs during retrieval.
     */
    public @Nullable Flat getFlat(@NotNull String name) {
        if (!existsFlat(name)) {
            return null;
        }
        return Objects.requireNonNull(allFlats.get(name),
                "Oops, something went terribly wrong. Please restart the server!");
    }

    /**
     * Retrieves a {@link Flat} that contains the specified {@link Location}.
     * <p>
     * This method searches through all loaded flats to find one whose defined
     * boundaries include the given location. If no matching flat is found,
     * {@code null} is returned.
     *
     * @param location The {@link Location} to check against all flats. Must not be null.
     * @return A {@link Flat} containing the specified {@link Location}, or {@code null} if no such flat exists.
     */
    public @Nullable Flat getFlatByLocation(@NotNull Location location) {
        return allFlats.values()
                .stream()
                .filter(flat -> flat.isWithinBounds(location))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets the owner of the specified flat.
     * <p>
     * This method updates the owner of the provided {@link Flat} instance
     * with the given {@link OfflinePlayer}. The ownership information is updated
     * within the flat object and is expected to be used for further state management,
     * such as storing or checking ownership.
     *
     * @param flat  The {@link Flat} instance for which the owner will be set.
     *              Cannot be null and should be a valid flat managed by the system.
     * @param owner The {@link OfflinePlayer} to be assigned as the owner of the flat.
     *              Can be null to indicate that the flat has no owner.
     */
    public void setOwner(Flat flat, OfflinePlayer owner) {
        flat.setOwner(owner);
    }

    /**
     * Adds a new {@link Area} to an existing flat.
     *
     * <p>
     * This method associates the given {@link Area} with the flat identified by its name. If a flat
     * with the specified name does not exist, an {@link IllegalArgumentException} is thrown.
     *
     * @param name The name of the flat to which the {@link Area} should be added. Must not be null.
     * @param area The {@link Area} to associate with the given flat. Must not be null.
     * @throws IllegalArgumentException If no flat exists with the specified name.
     */
    public void addArea(@NotNull String name, @NotNull Area area) throws IllegalArgumentException {
        if (!existsFlat(name)) {
            throw new IllegalArgumentException("No flat exists with the given name: " + name);
        }
        Flat flat = getFlat(name);
        //noinspection DataFlowIssue
        flat.addArea(area);
    }

    /**
     * Creates a new flat with the specified name and area.
     * <p>
     * This method attempts to create a new {@link Flat} instance using the provided name and {@link Area}.
     * If a flat with the same name already exists, an {@link IllegalArgumentException} is thrown.
     * The newly created flat is added to the internal storage.
     *
     * @param name The name of the flat to be created. Must not be null.
     * @param area The {@link Area} associated with the flat. Must not be null.
     * @throws IllegalArgumentException If a flat with the given {@code name} already exists.
     */
    public void create(@NotNull String name, @NotNull Area area) throws IllegalArgumentException {
        if (existsFlat(name)) {
            throw new IllegalArgumentException("A flat with this name already exists.");
        }
        Flat newFlat = new Flat(name, area);
        allFlats.put(name, newFlat);
    }

    /**
     * Deletes a flat identified by the given name from the system.
     * <p>
     * This method checks if a flat with the specified name exists using {@link #existsFlat(String)}.
     * If no such flat exists, it throws {@code IllegalArgumentException}.
     * Otherwise, the flat is removed from the internal storage.
     *
     * @param name The name of the flat to delete. Must not be null.
     * @throws IllegalArgumentException If no flat exists with the given name.
     */
    public void delete(@NotNull String name) throws IllegalArgumentException {
        if (!existsFlat(name)) {
            throw new IllegalArgumentException("No flat exists with the given name: " + name);
        }
        allFlats.remove(name);
    }

    /**
     * Checks if a flat with the specified name exists within the internal storage.
     * <p>
     * The method determines the existence of a flat by checking whether the specified
     * name is a key in the internal map of all flats.
     *
     * @param name The name of the flat to check for. Must not be null.
     * @return {@code true} if a flat with the specified name exists; {@code false} otherwise.
     */
    public boolean existsFlat(@NotNull String name) {
        return allFlats.containsKey(name);
    }

}