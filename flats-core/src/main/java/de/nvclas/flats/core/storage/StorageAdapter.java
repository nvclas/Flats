package de.nvclas.flats.core.storage;

import de.nvclas.flats.core.volumes.Area;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface defining persistent storage operations for flats, areas, and trusted players.
 */
public interface StorageAdapter extends AutoCloseable {

    /**
     * Initializes the connection to the storage system, preparing it for operations.
     * This method must be called before performing any storage-related operations.
     * <p>
     * Failure to invoke this method before interacting with the storage may result
     * in undefined behavior or exceptions.
     */
    void initConnection();

    /**
     * Migrates data to a new structure, format, or version within the storage system.
     * This operation may involve schema updates, data transformations, or other adjustments
     * necessary to ensure compatibility or alignment with updated storage requirements.
     * <p>
     * It is recommended to initialize the storage connection by calling {@link #initConnection()}
     * before invoking this method. Failure to do so may result in undefined behavior or exceptions.
     */
    void migrate();

    /**
     * Closes the storage connection and releases any allocated resources.
     */
    @Override
    void close();

    /**
     * Saves the specified {@link Flat} to persistent storage.
     * Existing database entries for the flat will be updated.
     *
     * @param flat The {@link Flat} to be saved. Must not be null.
     */
    void saveFlat(@NotNull Flat flat);

    /**
     * Loads a {@link Flat} by its name from persistent storage.
     *
     * @param name The name of the flat to load. Must not be null.
     * @return The {@link Flat} object if found; {@code null} otherwise.
     */
    @Nullable Flat loadFlat(@NotNull String name);

    /**
     * Deletes a flat from persistent storage based on its name.
     *
     * @param name The name of the flat to delete. Must not be null.
     */
    void deleteFlat(@NotNull String name);

    /**
     * Retrieves the number of flats owned by the specified player.
     *
     * @param player The {@link OfflinePlayer} whose owned flats are to be counted. Must not be null.
     * @return The total number of flats owned by the player.
     */
    int getOwnedFlatsCount(@NotNull OfflinePlayer player);

    /**
     * Checks whether the persistent storage is empty.
     *
     * @return {@code true} if storage contains no flats; {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * Checks whether a flat with the given name exists in storage.
     *
     * @param name The name of the flat to check. Must not be null.
     * @return {@code true} if the flat exists; {@code false} otherwise.
     */
    boolean existsFlat(@NotNull String name);

    /**
     * Retrieves the total count of flats currently stored.
     *
     * @return The total number of flats.
     */
    int getTotalFlatsCount();

    /**
     * Retrieves a paginated list of flat names from storage.
     *
     * @param offset The number of rows to skip.
     * @param limit  The maximum number of flat names to retrieve.
     * @return A list of flat names.
     */
    @NotNull List<String> getPaginatedFlatNames(int offset, int limit);

    /**
     * Retrieves a list of flat names that start with the specified prefix.
     *
     * @param prefix The prefix to filter flat names. Must not be null.
     * @param limit  The maximum number of flat names to return.
     * @return A list of flat names matching the specified prefix.
     */
    @NotNull List<String> getFilteredFlatNames(@NotNull String prefix, int limit);

    /**
     * Retrieves a list of areas that intersect with the specified rectangular boundaries within the given world.
     *
     * @param worldName The name of the world to query. Must not be null.
     * @param minX      The minimum x-coordinate.
     * @param maxX      The maximum x-coordinate.
     * @param minZ      The minimum z-coordinate.
     * @param maxZ      The maximum z-coordinate.
     * @return A list of intersecting {@link Area} objects.
     */
    @NotNull List<Area> getAreasIntersecting(@NotNull String worldName, int minX, int maxX, int minZ, int maxZ);
}
