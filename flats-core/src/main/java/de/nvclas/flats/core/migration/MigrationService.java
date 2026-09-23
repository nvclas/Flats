package de.nvclas.flats.core.migration;

/**
 * Service for migrating old configs and data to the latest version.
 */
public interface MigrationService {

    /**
     * Executes the migration process, updating old configuration files and associated data
     * structures to conform to the current version's schema.
     * <p>
     * This method should be called upon initial plugin enablement to ensure data compatibility.
     */
    void migrate();

}
