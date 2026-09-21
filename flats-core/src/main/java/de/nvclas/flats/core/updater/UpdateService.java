package de.nvclas.flats.core.updater;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for performing plugin update operations
 */
public interface UpdateService {

    /**
     * Asynchronously performs an update operation.
     * <p>
     * The operation result is returned via a {@link CompletableFuture}.
     *
     * @return A {@link CompletableFuture} that will complete with the {@link UpdateResult} indicating the status of the update.
     */
    @NotNull CompletableFuture<UpdateResult> updateAsync();
}
