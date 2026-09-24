package de.nvclas.flats.core.storage;

import de.nvclas.flats.core.BaseFlats;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteStorage extends AbstractSqlStorage {

    private static final String DATABASE_DIR = "db/migrations/sqlite";

    @Getter
    private final String fileName;

    public SqliteStorage(@NotNull BaseFlats plugin) {
        super(plugin);
        this.fileName = plugin.getConfigAdapter().getSqliteFileName();
    }

    @Override
    protected @NotNull String getJdbcUrl() {
        return "jdbc:sqlite:" + new File(plugin.getDataFolder(), fileName).getAbsolutePath();
    }

    @Override
    protected @NotNull String getMigrationLocation() {
        return DATABASE_DIR;
    }

    @Override
    protected void configureConnection(@NotNull Connection connection) throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            throw new IllegalStateException("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
        }

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
    }

    @Override
    protected @NotNull String getUpsertFlatSql() {
        return """
                INSERT INTO flats (name, owner_uuid) VALUES (?, ?)
                ON CONFLICT(name COLLATE NOCASE) DO UPDATE SET owner_uuid = EXCLUDED.owner_uuid""";
    }

    @Override
    protected @NotNull String getLoadFlatMetadataSql() {
        return "SELECT name, owner_uuid FROM flats WHERE name = ? COLLATE NOCASE";
    }

    @Override
    protected @NotNull String getDeleteFlatSql() {
        return "DELETE FROM flats WHERE name = ? COLLATE NOCASE";
    }

    @Override
    protected @NotNull String getExistsFlatSql() {
        return "SELECT 1 FROM flats WHERE name = ? COLLATE NOCASE";
    }

    @Override
    protected @NotNull String getFilteredFlatNamesSql() {
        return "SELECT name FROM flats WHERE name LIKE ? ESCAPE '\\' ORDER BY name COLLATE NOCASE LIMIT ?";
    }
}
