package de.nvclas.flats.storage;

import de.nvclas.flats.Flats;
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
