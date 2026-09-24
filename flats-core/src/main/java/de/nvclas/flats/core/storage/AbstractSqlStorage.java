package de.nvclas.flats.core.storage;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.volumes.Area;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

@SuppressWarnings({"SqlSourceToSinkFlow", "java:S2077"}) // SQL comes from internal classes, they are safe
public abstract class AbstractSqlStorage implements StorageAdapter {

    protected final BaseFlats plugin;
    protected Connection connection;

    protected AbstractSqlStorage(@NotNull BaseFlats plugin) {
        this.plugin = plugin;
    }

    protected abstract @NotNull String getJdbcUrl();

    protected abstract @NotNull String getMigrationLocation();

    protected abstract @NotNull String getUpsertFlatSql();

    protected @NotNull String getLoadFlatMetadataSql() {
        return "SELECT name, owner_uuid FROM flats WHERE name = ?";
    }

    protected @NotNull String getDeleteFlatSql() {
        return "DELETE FROM flats WHERE name = ?";
    }

    protected @NotNull String getExistsFlatSql() {
        return "SELECT 1 FROM flats WHERE name = ?";
    }

    protected @NotNull String getFilteredFlatNamesSql() {
        return "SELECT name FROM flats WHERE name LIKE ? ESCAPE '\\' ORDER BY name LIMIT ?";
    }

    protected @Nullable String getUsername() {
        return null;
    }

    protected @Nullable String getPassword() {
        return null;
    }

    protected void configureConnection(@NotNull Connection connection) throws SQLException {
        // default: no database-specific setup
    }

    @Override
    public void initConnection() {
        try {
            String username = getUsername();
            if (username != null) {
                connection = DriverManager.getConnection(getJdbcUrl(), username, getPassword());
            } else {
                connection = DriverManager.getConnection(getJdbcUrl());
            }

            configureConnection(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize database connection", e);
        }
    }

    @Override
    public void migrate() {
        Flyway flyway = Flyway.configure(plugin.getClass().getClassLoader())
                .dataSource(getJdbcUrl(), getUsername(), getPassword())
                .baselineOnMigrate(true)
                .locations(getMigrationLocation())
                .mixed(true)
                .load();

        try {
            flyway.migrate();
        } catch (FlywayException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Database migration failed");
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not close database connection");
        }
    }

    @Override
    public synchronized void saveFlat(@NotNull Flat flat) {
        try {
            connection.setAutoCommit(false);

            upsertFlatMetadata(flat);
            replaceAreas(flat);
            replaceTrustedPlayers(flat);

            connection.commit();
        } catch (SQLException e) {
            rollbackTransaction(e);
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not save flat " + flat.getName());
        } finally {
            resetAutoCommit();
        }
    }

    private void upsertFlatMetadata(@NotNull Flat flat) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(getUpsertFlatSql())) {
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
            plugin.getLogger()
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
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not set auto-commit to true");
        }
    }

    @Override
    public synchronized @Nullable Flat loadFlat(@NotNull String name) {
        try {
            FlatMetadata metadata = loadMetadata(name);
            if (metadata == null) {
                return null;
            }

            List<Area> areas = loadAreas(metadata.name());
            List<OfflinePlayer> trusted = loadTrustedPlayers(metadata.name());

            return new Flat(metadata.name(), metadata.owner(), areas, trusted);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not load flat " + name);
            return null;
        }
    }

    private @Nullable FlatMetadata loadMetadata(@NotNull String flatName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(getLoadFlatMetadataSql())) {
            ps.setString(1, flatName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String uuidStr = rs.getString("owner_uuid");
                    OfflinePlayer owner = uuidStr != null && !uuidStr.isEmpty()
                            ? Bukkit.getOfflinePlayer(UUID.fromString(uuidStr))
                            : null;
                    return new FlatMetadata(name, owner);
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

    @Override
    public synchronized void deleteFlat(@NotNull String name) {
        try (PreparedStatement ps = connection.prepareStatement(getDeleteFlatSql())) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not delete flat " + name);
        }
    }

    @Override
    public synchronized int getOwnedFlatsCount(@NotNull OfflinePlayer player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM flats WHERE owner_uuid = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not get owned flats count for " + player.getName());
        }
        return 0;
    }

    @Override
    public synchronized boolean isEmpty() {
        return getTotalFlatsCount() == 0;
    }

    @Override
    public synchronized boolean existsFlat(@NotNull String name) {
        try (PreparedStatement ps = connection.prepareStatement(getExistsFlatSql())) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not check if flat exists: " + name);
        }
        return false;
    }

    @Override
    public synchronized int getTotalFlatsCount() {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM flats")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not get total flats count");
        }
        return 0;
    }

    @Override
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
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not get paginated flat names");
        }
        return names;
    }

    @Override
    public synchronized @NotNull List<String> getFilteredFlatNames(@NotNull String prefix, int limit) {
        List<String> names = new ArrayList<>();
        String escapedPrefix = escapeLikePattern(prefix);
        try (PreparedStatement ps = connection.prepareStatement(getFilteredFlatNamesSql())) {
            ps.setString(1, escapedPrefix + "%");
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not get filtered flat names");
        }
        return names;
    }

    protected @NotNull String escapeLikePattern(@NotNull String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
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
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not get areas intersecting " + worldName);
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

    private record FlatMetadata(@NotNull String name, @Nullable OfflinePlayer owner) {
    }
}
