package de.nvclas.flats.storage;

import de.nvclas.flats.Flats;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles persistent storage for flats using SQLite.
 * This class manages the database connection, table initialization,
 * and provides CRUD operations for flats, areas, and trusted players.
 */
public class FlatsStorage {

    public static final String DATABASE_NAME = "flats.db";
    private static final String DATABASE_DIR = "database";
    private final Flats flatsPlugin;
    private Connection connection;

    public FlatsStorage(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
        initConnection();
        migrate();
    }

    private void initConnection() {
        File dataFolder = flatsPlugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            throw new IllegalStateException("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
        }

        try {
            String url = getJdbcUrl();
            connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON;");
                statement.execute("PRAGMA busy_timeout = 3000;");
                statement.execute("PRAGMA journal_mode = WAL;");
                statement.execute("PRAGMA synchronous = NORMAL;");
                statement.execute("PRAGMA temp_store = MEMORY;");
                statement.execute("PRAGMA cache_size = -16000;");
                statement.execute("PRAGMA mmap_size = 67108864;");
                statement.execute("PRAGMA analysis_limit = 400;");
                statement.execute("PRAGMA optimize;");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize database connection", e);
        }
    }

    private @NotNull String getJdbcUrl() {
        return "jdbc:sqlite:" + new File(flatsPlugin.getDataFolder(), DATABASE_NAME).getAbsolutePath();
    }

    private void migrate() {
        Flyway flyway = Flyway.configure(flatsPlugin.getClass().getClassLoader())
                .dataSource(getJdbcUrl(), null, null)
                .baselineOnMigrate(true)
                .locations(DATABASE_DIR)
                .mixed(true)
                .load();
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Database migration failed");
            Bukkit.getPluginManager().disablePlugin(flatsPlugin);
        }
    }

    /**
     * Closes the database connection associated with this storage instance.
     * <p>
     * If the connection is already closed or null, this method does nothing.
     * Logs an error if a {@link SQLException} occurs while closing the connection.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not close database connection");
        }
    }

    /**
     * Saves the specified {@link Flat} to the database. This includes persisting the flat's
     * name, owner, associated areas, and trusted players. Existing database entries for
     * the flat will be updated.
     *
     * <p>Database operations are encapsulated in a transaction. If an error occurs, the changes
     * are rolled back.
     *
     * @param flat The {@link Flat} to be saved. Must not be null.
     */
    public synchronized void saveFlat(@NotNull Flat flat) {
        try {
            connection.setAutoCommit(false);

            upsertFlatMetadata(flat);
            replaceAreas(flat);
            replaceTrustedPlayers(flat);

            connection.commit();
        } catch (SQLException e) {
            rollbackTransaction(e);
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not save flat " + flat.getName());
        } finally {
            resetAutoCommit();
        }
    }

    private void upsertFlatMetadata(@NotNull Flat flat) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO flats (name, owner_uuid) VALUES (?, ?)
                ON CONFLICT(name) DO UPDATE SET owner_uuid = EXCLUDED.owner_uuid""")) {
            ps.setString(1, flat.getName());
            ps.setString(2, flat.getOwner() == null ? null : flat.getOwner().getUniqueId().toString());
            ps.executeUpdate();
        }
    }

    private void replaceAreas(@NotNull Flat flat) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM areas WHERE flat_name = ?")) {
            ps.setString(1, flat.getName());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO areas (flat_name, world, min_x, min_y, min_z, max_x, max_y, max_z) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Area area : flat.getAreas()) {
                ps.setString(1, flat.getName());
                ps.setString(2, area.getWorldName());
                ps.setInt(3, area.getMinX());
                ps.setInt(4, area.getMinY());
                ps.setInt(5, area.getMinZ());
                ps.setInt(6, area.getMaxX());
                ps.setInt(7, area.getMaxY());
                ps.setInt(8, area.getMaxZ());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void replaceTrustedPlayers(@NotNull Flat flat) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM trusted WHERE flat_name = ?")) {
            ps.setString(1, flat.getName());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO trusted (flat_name, player_uuid) VALUES (?, ?)")) {
            for (OfflinePlayer player : flat.getTrusted()) {
                ps.setString(1, flat.getName());
                ps.setString(2, player.getUniqueId().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void rollbackTransaction(@NotNull SQLException originalException) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger()
                    .log(Level.SEVERE, e,
                            () -> "Could not rollback transaction after error: " + originalException.getMessage());
        }
    }

    private void resetAutoCommit() {
        try {
            if (connection != null) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not set auto-commit to true");
        }
    }

    /**
     * Loads a {@link Flat} by its name from persistent storage.
     * <p>
     * If the specified flat does not exist, {@code null} is returned.
     * Trusted players and associated areas are also loaded as part of the flat.
     *
     * @param name The name of the flat to load. Must not be {@code null}.
     * @return A {@link Flat} object if the flat exists; {@code null} otherwise.
     */
    public synchronized @Nullable Flat loadFlat(@NotNull String name) {
        try {
            FlatMetadata metadata = loadMetadata(name);
            if (metadata == null) {
                return null;
            }

            List<Area> areas = loadAreas(name);
            List<OfflinePlayer> trusted = loadTrustedPlayers(name);

            return new Flat(name, metadata.owner(), areas, trusted);
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not load flat " + name);
            return null;
        }
    }

    private @Nullable FlatMetadata loadMetadata(@NotNull String flatName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT owner_uuid FROM flats WHERE name = ?")) {
            ps.setString(1, flatName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String uuidStr = rs.getString("owner_uuid");
                    OfflinePlayer owner = (uuidStr != null && !uuidStr.isEmpty())
                            ? Bukkit.getOfflinePlayer(UUID.fromString(uuidStr))
                            : null;
                    return new FlatMetadata(true, owner);
                }
            }
        }
        return null;
    }

    private @NotNull List<Area> loadAreas(@NotNull String flatName) throws SQLException {
        List<Area> areas = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, world, min_x, min_y, min_z, max_x, max_y, max_z FROM areas WHERE flat_name = ?")) {
            ps.setString(1, flatName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    areas.add(mapResultSetToArea(rs, flatName));
                }
            }
        }
        return areas;
    }

    private @NotNull List<OfflinePlayer> loadTrustedPlayers(@NotNull String flatName) throws SQLException {
        List<OfflinePlayer> trusted = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_uuid FROM trusted WHERE flat_name = ?")) {
            ps.setString(1, flatName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String uuidStr = rs.getString("player_uuid");
                    if (uuidStr != null && !uuidStr.isEmpty()) {
                        trusted.add(Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)));
                    }
                }
            }
        }
        return trusted;
    }

    /**
     * Deletes a flat from the database based on its name.
     * <p>
     * This operation removes the flat record with the specified name from persistent storage.
     *
     * @param name the name of the flat to delete; must not be {@code null}.
     */
    public synchronized void deleteFlat(@NotNull String name) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM flats WHERE name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not delete flat " + name);
        }
    }

    /**
     * Retrieves the number of flats owned by the specified player.
     * <p>
     * This method queries the database to determine how many flats are owned by the given player.
     *
     * @param player The {@link OfflinePlayer} whose owned flats are to be counted. Must not be {@code null}.
     * @return The total number of flats owned by the specified player, or {@code 0} if none are found or an error occurs.
     */
    public synchronized int getOwnedFlatsCount(@NotNull OfflinePlayer player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM flats WHERE owner_uuid = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger()
                    .log(Level.SEVERE, e, () -> "Could not get owned flats count for " + player.getName());
        }
        return 0;
    }

    /**
     * Checks whether the database table storing flat data is empty.
     * <p>
     * This method queries the database and returns {@code true} if there are no records in the table.
     * In case of an error during the database access, the method returns {@code true} as a fallback.
     *
     * @return {@code true} if the database is empty or an error occurs; {@code false} otherwise.
     */
    public synchronized boolean isEmpty() {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM flats")) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not check if database is empty");
        }
        return true;
    }

    /**
     * Checks whether a flat with the given name exists in the database.
     * <p>
     * This method queries the database to determine the existence of a flat
     * with the specified name.
     *
     * @param name the name of the flat to check; must not be {@code null}.
     * @return {@code true} if a flat with the given name exists, {@code false} otherwise.
     */
    public synchronized boolean existsFlat(@NotNull String name) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM flats WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not check if flat exists: " + name);
        }
        return false;
    }

    /**
     * Retrieves the total count of flats in the database.
     *
     * <p>
     * Executes a query on the database to calculate and return the number of
     * flats currently stored. If an error occurs during the query, logs the
     * exception and returns {@code 0}.
     *
     * @return The total number of flats, or {@code 0} if an error occurs.
     */
    public synchronized int getTotalFlatsCount() {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM flats")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not get total flats count");
        }
        return 0;
    }

    /**
     * Retrieves a paginated list of flat names from the database.
     * <p>
     * The results are ordered by name and constrained by the provided offset and limit.
     *
     * @param offset The number of rows to skip before starting to retrieve results.
     * @param limit  The maximum number of flat names to retrieve.
     * @return A list of flat names, or an empty list if no results are found.
     */
    public synchronized @NotNull List<String> getPaginatedFlatNames(int offset, int limit) {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM flats ORDER BY name LIMIT ? OFFSET ?")) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not get paginated flat names");
        }
        return names;
    }

    /**
     * Retrieves a list of flat names that start with the specified prefix, limited to a specific number of results.
     *
     * <p>
     * This method fetches flat names from the database that match the given prefix and enforces a maximum result limit.
     *
     * @param prefix The prefix to filter flat names. Must not be {@code null}.
     * @param limit  The maximum number of flat names to return. Must be a positive integer.
     * @return A list of flat names matching the specified prefix. Returns an empty list if no matching names are found.
     */
    public synchronized @NotNull List<String> getFilteredFlatNames(@NotNull String prefix, int limit) {
        List<String> names = new ArrayList<>();
        String escapedPrefix = escapeLikePattern(prefix);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM flats WHERE name LIKE ? ESCAPE '\\' ORDER BY name COLLATE NOCASE LIMIT ?")) {
            ps.setString(1, escapedPrefix + "%");
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not get filtered flat names");
        }
        return names;
    }

    private @NotNull String escapeLikePattern(@NotNull String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Retrieves a list of areas that intersect with the specified rectangular boundaries within the given world.
     *
     * @param worldName the name of the world to query for intersecting areas.
     * @param minX      the minimum x-coordinate of the boundary.
     * @param maxX      the maximum x-coordinate of the boundary.
     * @param minZ      the minimum z-coordinate of the boundary.
     * @param maxZ      the maximum z-coordinate of the boundary.
     * @return a list of {@link Area} objects representing the intersecting areas. If no areas intersect, an empty list is returned.
     */
    public synchronized @NotNull List<Area> getAreasIntersecting(@NotNull String worldName, int minX, int maxX,
            int minZ, int maxZ) {
        List<Area> areas = new ArrayList<>();
        String sql = """
                SELECT flat_name, world, min_x, min_y, min_z, max_x, max_y, max_z FROM areas WHERE world = ?
                  AND max_x >= ?
                  AND min_x <= ?
                  AND max_z >= ?
                  AND min_z <= ?""";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, worldName);
            ps.setInt(2, minX);
            ps.setInt(3, maxX);
            ps.setInt(4, minZ);
            ps.setInt(5, maxZ);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    areas.add(mapResultSetToArea(rs, rs.getString("flat_name")));
                }
            }
        } catch (SQLException e) {
            flatsPlugin.getLogger().log(Level.SEVERE, e, () -> "Could not get areas intersecting " + worldName);
        }
        return areas;
    }

    private @NotNull Area mapResultSetToArea(@NotNull ResultSet rs, @NotNull String flatName) throws SQLException {
        String worldName = rs.getString("world");
        int minX = rs.getInt("min_x");
        int minY = rs.getInt("min_y");
        int minZ = rs.getInt("min_z");
        int maxX = rs.getInt("max_x");
        int maxY = rs.getInt("max_y");
        int maxZ = rs.getInt("max_z");

        return Area.fromRawData(worldName, new Area.Bounds(minX, maxX, minY, maxY, minZ, maxZ), flatName);
    }

    private record FlatMetadata(boolean exists, @Nullable OfflinePlayer owner) {
    }
}
