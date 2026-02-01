package de.nvclas.flats.storage;

import de.nvclas.flats.Flats;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private final Flats plugin;
    private Connection connection;

    private static final String INIT_SQL = "db/init.sql";
    private static final String DATABASE_NAME = "flats.db";

    public FlatsStorage(Flats plugin) {
        this.plugin = plugin;
        initConnection();
        initTables();
    }

    private void initConnection() {
        try {
            String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/" + DATABASE_NAME;
            connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not initialize database connection");
        }
    }

    private void initTables() {
        try (InputStream is = plugin.getResource(INIT_SQL)) {
            if (is == null) {
                plugin.getLogger().log(Level.SEVERE, () -> "Could not find " + INIT_SQL + " in resources");
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is)); Statement statement = connection.createStatement()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                    if (line.trim().endsWith(";")) {
                        statement.execute(sb.toString());
                        sb.setLength(0);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not initialize database tables");
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not close database connection");
        }
    }

    public void saveFlat(@NotNull Flat flat) {
        try {
            connection.setAutoCommit(false);

            // Upsert flat
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO flats (name, owner_uuid) VALUES (?, ?) "
                            + "ON CONFLICT(name) DO UPDATE SET owner_uuid = EXCLUDED.owner_uuid")) {
                ps.setString(1, flat.getName());
                ps.setString(2, flat.getOwner() == null ? null : flat.getOwner().getUniqueId().toString());
                ps.executeUpdate();
            }

            // Replace areas
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM areas WHERE flat_name = ?")) {
                ps.setString(1, flat.getName());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO areas (flat_name, world, min_x, min_y, min_z, max_x, max_y, max_z) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (Area area : flat.getAreas()) {
                    ps.setString(1, flat.getName());
                    ps.setString(2, area.getPos1().getWorld().getName());
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

            // Replace trusted
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

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, e, () -> "Could not rollback transaction");
            }
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not save flat " + flat.getName());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, e, () -> "Could not set auto-commit to true");
            }
        }
    }

    public @Nullable Flat loadFlat(@NotNull String name) {
        try {
            OfflinePlayer owner = null;
            try (PreparedStatement ps = connection.prepareStatement("SELECT owner_uuid FROM flats WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String uuidStr = rs.getString("owner_uuid");
                        if (uuidStr != null && !uuidStr.isEmpty()) {
                            owner = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                        }
                    } else {
                        return null; // Flat not found
                    }
                }
            }

            List<Area> areas = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, flat_name, world, min_x, min_y, min_z, max_x, max_y, max_z FROM areas WHERE flat_name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        addAreas(areas, rs, name);
                    }
                }
            }

            List<OfflinePlayer> trusted = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT player_uuid FROM trusted WHERE flat_name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        trusted.add(Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("player_uuid"))));
                    }
                }
            }

            return new Flat(name, owner, areas, trusted);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not load flat " + name);
            return null;
        }
    }

    public void deleteFlat(@NotNull String name) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM flats WHERE name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not delete flat " + name);
        }
    }

    public List<String> getAllFlatNames() {
        List<String> names = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(
                "SELECT name FROM flats")) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not get all flat names");
        }
        return names;
    }

    public List<Area> loadAllAreas() {
        List<Area> areas = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(
                "SELECT id, flat_name, world, min_x, min_y, min_z, max_x, max_y, max_z FROM areas")) {
            while (rs.next()) {
                String flatName = rs.getString("flat_name");
                addAreas(areas, rs, flatName);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not load all areas");
        }
        return areas;
    }

    public int getOwnedFlatsCount(@NotNull OfflinePlayer player) {
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

    public boolean isEmpty() {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(
                "SELECT COUNT(*) FROM flats")) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e, () -> "Could not check if database is empty");
        }
        return true;
    }

    private void addAreas(List<Area> areas, ResultSet rs, String flatName) throws SQLException {
        String worldName = rs.getString("world");
        int minX = rs.getInt("min_x");
        int minY = rs.getInt("min_y");
        int minZ = rs.getInt("min_z");
        int maxX = rs.getInt("max_x");
        int maxY = rs.getInt("max_y");
        int maxZ = rs.getInt("max_z");

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Location pos1 = new Location(world, minX, minY, minZ);
            Location pos2 = new Location(world, maxX, maxY, maxZ);
            areas.add(new Area(pos1, pos2, flatName));
        }
    }

}
