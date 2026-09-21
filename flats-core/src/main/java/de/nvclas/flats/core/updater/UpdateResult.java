package de.nvclas.flats.core.updater;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record UpdateResult(UpdateStatus status, String latestVersion, String fileName, String downloadUrl) {

    public static @NotNull UpdateResult status(@NotNull UpdateStatus status) {
        return new UpdateResult(status, null, null, null);
    }

    public static @NotNull UpdateResult success(@NotNull String fileName, @Nullable String latestVersion) {
        return new UpdateResult(UpdateStatus.SUCCESS, latestVersion, fileName, null);
    }

    public static @NotNull UpdateResult available(@NotNull String latestVersion, @NotNull String downloadUrl) {
        return new UpdateResult(UpdateStatus.UPDATE_AVAILABLE, latestVersion, null, downloadUrl);
    }
}
