package de.nvclas.flats.storage;

import de.nvclas.flats.Flats;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.MockBukkitExtension;
import org.mockbukkit.mockbukkit.MockBukkitInject;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockBukkitExtension.class)
@DisplayName("FlatsStorage Migration Tests")
class FlatsStorageTest {

    @MockBukkitInject
    private ServerMock server;
    @MockBukkitInject
    private Flats plugin;

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Loading a flat with areas in an unloaded world preserves all area data")
    void testLoadFlatWithUnloadedWorld() {
        // Bukkit.getWorld("unloaded_world") returns null since we never register this world in MockBukkit.
        // Area.fromRawData() should create an Area that holds the world name and coordinates in memory,
        // with pos1/pos2 carrying a null world reference.
        String worldName = "unloaded_world";
        Area area = Area.fromRawData(worldName, 0, 0, 0, 10, 20, 10, "test_flat");

        assertNull(area.getPos1().getWorld(), "World reference should be null for an unloaded world");
        assertEquals(worldName, area.getWorldName(), "World name should be preserved even without a loaded world");

        // Save the flat — saveFlat() uses area.getWorldName(), so no live World is required
        Flat flat = new Flat("test_flat", area);
        plugin.getFlatsStorage().saveFlat(flat);

        // Load it back: the critical assertion is that the flat is returned with its areas intact,
        // not silently dropped because the world is currently unloaded
        Flat loaded = plugin.getFlatsStorage().loadFlat("test_flat");

        assertNotNull(loaded, "Flat should be loaded even when its world is not currently loaded");
        assertEquals(1, loaded.getAreas().size(), "All areas should be preserved when the world is not loaded");

        Area loadedArea = loaded.getAreas().get(0);
        assertEquals(worldName, loadedArea.getWorldName(), "World name should be preserved after load");
        assertEquals(0, loadedArea.getMinX(), "minX should be preserved");
        assertEquals(10, loadedArea.getMaxX(), "maxX should be preserved");
        assertEquals(0, loadedArea.getMinY(), "minY should be preserved");
        assertEquals(20, loadedArea.getMaxY(), "maxY should be preserved");
        assertEquals(0, loadedArea.getMinZ(), "minZ should be preserved");
        assertEquals(10, loadedArea.getMaxZ(), "maxZ should be preserved");
        assertNull(loadedArea.getPos1().getWorld(), "World reference should remain null for an unloaded world");

        // getAllOuterBlocks() must return empty safely, not throw a NullPointerException
        assertTrue(loadedArea.getAllOuterBlocks().isEmpty(),
                "getAllOuterBlocks() should return empty for an area with an unloaded world");
    }

    @Test
    @DisplayName("Verify that Flyway migrations are correctly applied")
    void testMigrationsApplied() throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), FlatsStorage.DATABASE_NAME);
        assertTrue(dbFile.exists(), "Database file should exist after migration");

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()) {

            // Check if tables from initial migration exist
            assertTableExists(stmt, "flats");
            assertTableExists(stmt, "areas");
            assertTableExists(stmt, "trusted");

            // Check if indexes exist
            assertIndexExists(stmt, "idx_flats_owner_uuid");
            assertIndexExists(stmt, "idx_areas_flat_name");
            assertIndexExists(stmt, "idx_areas_spatial");

            // Check if Flyway metadata table exists
            assertTableExists(stmt, "flyway_schema_history");

            // Check if migration version 2.0.0 is successful
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT success FROM flyway_schema_history WHERE version = '2.0.0'")) {
                assertTrue(rs.next(), "Migration version 2.0.0 should be recorded in flyway_schema_history");
                assertTrue(rs.getBoolean("success"), "Migration version 2.0.0 should be successful");
            }
        }
    }

    private void assertTableExists(Statement stmt, String tableName) throws SQLException {
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";
        try (ResultSet rs = stmt.executeQuery(query)) {
            assertTrue(rs.next(), "Table '" + tableName + "' should exist in the database");
        }
    }

    private void assertIndexExists(Statement stmt, String indexName) throws SQLException {
        String query = "SELECT name FROM sqlite_master WHERE type='index' AND name='" + indexName + "'";
        try (ResultSet rs = stmt.executeQuery(query)) {
            assertTrue(rs.next(), "Index '" + indexName + "' should exist in the database");
        }
    }
}
