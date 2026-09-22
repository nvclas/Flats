package de.nvclas.flats.lite.updater;

import com.google.gson.Gson;
import de.nvclas.flats.core.updater.UpdateResult;
import de.nvclas.flats.core.updater.UpdateService;
import de.nvclas.flats.core.updater.UpdateStatus;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * Manages checking and downloading plugin updates from GitHub releases.
 */
@RequiredArgsConstructor
public class GitHubUpdateService implements UpdateService {

    private static final String UPDATE_PROCESS_ERROR = "An error occurred during the update process";
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    private final JavaPlugin plugin;
    private final String apiUrl;

    @Override
    public @NotNull CompletableFuture<UpdateResult> updateAsync() {
        return fetchLatestReleaseAsync().thenCompose(this::processRelease).exceptionally(e -> {
            Throwable cause = (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
            logException(UPDATE_PROCESS_ERROR, cause);
            return UpdateResult.status(UpdateStatus.FAILED);
        });
    }

    private @NotNull CompletableFuture<ReleaseInfo> fetchLatestReleaseAsync() {
        HttpRequest request = createGitHubApiRequest(apiUrl);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> switch (response.statusCode()) {
                    case 200 -> parseReleaseInfo(response.body());
                    case 404 -> ReleaseInfo.notFound();
                    case 403 -> {
                        plugin.getLogger().log(Level.WARNING, "GitHub API rate limit exceeded or forbidden access.");
                        yield ReleaseInfo.notFound();
                    }
                    default -> throw new CompletionException(
                            new IOException("Failed to fetch latest release: HTTP " + response.statusCode()));
                });
    }

    private @NotNull CompletableFuture<UpdateResult> processRelease(@NotNull ReleaseInfo releaseInfo) {
        if (!releaseInfo.exists()) {
            return CompletableFuture.completedFuture(UpdateResult.status(UpdateStatus.NOT_FOUND));
        }

        String latestVersion = releaseInfo.version();
        String fileName = releaseInfo.fileName();
        String currentVersion = plugin.getPluginMeta().getVersion();

        if (Objects.equals(currentVersion, latestVersion)) {
            logVersionStatus(currentVersion, latestVersion, true);
            return CompletableFuture.completedFuture(UpdateResult.status(UpdateStatus.ALREADY_UP_TO_DATE));
        }

        logVersionStatus(currentVersion, latestVersion, false);
        return downloadJarAsync(releaseInfo.downloadUrl(), fileName).thenApply(
                tempFile -> moveToUpdateFolder(tempFile, fileName, latestVersion));
    }

    private @NotNull ReleaseInfo parseReleaseInfo(@NotNull String responseBody) {
        GitHubRelease release = GSON.fromJson(responseBody, GitHubRelease.class);
        if (release == null || release.tag_name() == null || release.assets() == null) {
            return ReleaseInfo.notFound();
        }

        String tagName = release.tag_name();
        String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        plugin.getLogger().log(Level.INFO, () -> "Latest version: " + version);

        for (GitHubAsset asset : release.assets()) {
            if (asset.name() != null && asset.name().endsWith(".jar") && asset.browser_download_url() != null) {
                plugin.getLogger().log(Level.INFO, () -> "Fetched latest release URL: " + asset.browser_download_url());
                return new ReleaseInfo(version, asset.name(), asset.browser_download_url());
            }
        }

        plugin.getLogger().log(Level.WARNING, "No JAR asset found in the latest release.");
        return ReleaseInfo.notFound();
    }

    private @NotNull CompletableFuture<Path> downloadJarAsync(@NotNull String downloadUrl,
            @NotNull String targetFileName) {
        Path tempFile;
        try {
            tempFile = createTempDownloadFile(targetFileName);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        HttpRequest request = createDownloadRequest(downloadUrl);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(tempFile)).thenApply(response -> {
            if (response.statusCode() != 200) {
                deleteQuietly(tempFile);
                throw new CompletionException(
                        new IOException("Failed to download file: HTTP " + response.statusCode()));
            }

            try {
                long downloadedSize = Files.size(tempFile);
                if (downloadedSize <= 0) {
                    deleteQuietly(tempFile);
                    throw new IOException("Downloaded file is empty");
                }

                long expectedLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);

                if (expectedLength != -1L && expectedLength != downloadedSize) {
                    deleteQuietly(tempFile);
                    throw new IOException(
                            "Downloaded file is incomplete. Expected " + expectedLength + " bytes but got "
                                    + downloadedSize + " bytes");
                }

                plugin.getLogger().log(Level.INFO, () -> "Download completed: " + tempFile.getFileName());
                return tempFile;
            } catch (IOException e) {
                deleteQuietly(tempFile);
                throw new CompletionException(e);
            }
        });
    }

    private @NotNull HttpRequest createGitHubApiRequest(@NotNull String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Flats-Plugin-Updater")
                .GET()
                .build();
    }

    private @NotNull HttpRequest createDownloadRequest(@NotNull String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "Flats-Plugin-Updater")
                .GET()
                .build();
    }

    private @NotNull Path createTempDownloadFile(@NotNull String targetFileName) throws IOException {
        Path tempDir = plugin.getDataFolder().toPath().resolve("updates");
        Files.createDirectories(tempDir);
        String sanitizedName = targetFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return Files.createTempFile(tempDir, "download-", "-" + sanitizedName);
    }

    private @NotNull UpdateResult moveToUpdateFolder(@NotNull Path tempFile, @Nullable String targetFileName,
            @Nullable String latestVersion) {
        File updateFolder = plugin.getServer().getUpdateFolderFile();
        if (targetFileName == null || targetFileName.isBlank()) {
            plugin.getLogger().log(Level.SEVERE, "Could not resolve target file name");
            deleteQuietly(tempFile);
            return UpdateResult.status(UpdateStatus.FAILED);
        }

        Path targetPath = updateFolder.toPath().resolve(targetFileName);
        try {
            Files.createDirectories(updateFolder.toPath());
            Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger()
                    .log(Level.INFO, () -> "Update downloaded to update folder: " + targetPath
                            + ". Will apply on next restart.");
            return UpdateResult.success(targetFileName, latestVersion);
        } catch (IOException e) {
            logException("Failed to move file to update folder", e);
            deleteQuietly(tempFile);
            return UpdateResult.status(UpdateStatus.FAILED);
        }
    }

    private void logVersionStatus(@NotNull String currentVersion, @NotNull String latestVersion, boolean isUpToDate) {
        if (isUpToDate) {
            plugin.getLogger().log(Level.INFO, () -> "Current version " + currentVersion + " is already up to date");
        } else {
            plugin.getLogger()
                    .log(Level.INFO, () -> "Updating from version " + currentVersion + " to " + latestVersion);
        }
    }

    private void deleteQuietly(@NotNull Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logException("Failed to delete temporary update file", e);
        }
    }

    private void logException(@NotNull String message, @NotNull Throwable e) {
        plugin.getLogger().log(Level.SEVERE, e, () -> message + ": " + e.getMessage());
    }

    private record ReleaseInfo(String version, String fileName, String downloadUrl) {
        private static ReleaseInfo notFound() {
            return new ReleaseInfo(null, null, null);
        }

        private boolean exists() {
            return version != null && fileName != null && downloadUrl != null;
        }
    }

    private record GitHubRelease(String tag_name, List<GitHubAsset> assets) {
    }

    private record GitHubAsset(String name, String browser_download_url) {
    }
}
