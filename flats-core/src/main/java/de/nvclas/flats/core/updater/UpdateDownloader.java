package de.nvclas.flats.core.updater;

import com.google.gson.Gson;
import lombok.Getter;
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
 * Manages downloading plugin updates from GitHub and placing them into the server's update directory.
 */
public class UpdateDownloader {

    private static final String UPDATE_PROCESS_ERROR = "An error occurred during the update process";
    private static final Gson GSON = new Gson();
    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private final String apiUrl;

    @Getter
    private String fileName;

    @Getter
    private String latestVersion;

    public UpdateDownloader(JavaPlugin plugin, String apiUrl) {
        this.plugin = plugin;
        this.apiUrl = apiUrl;
        this.httpClient = DEFAULT_HTTP_CLIENT;
    }

    public @NotNull CompletableFuture<UpdateStatus> downloadLatestReleaseAsync() {
        return fetchLatestReleaseAsync()
                .thenCompose(this::processRelease)
                .exceptionally(e -> {
                    Throwable cause = (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
                    logException(UPDATE_PROCESS_ERROR, cause);
                    return UpdateStatus.FAILED;
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

    private @NotNull CompletableFuture<UpdateStatus> processRelease(@NotNull ReleaseInfo releaseInfo) {
        if (!releaseInfo.exists()) {
            return CompletableFuture.completedFuture(UpdateStatus.NOT_FOUND);
        }

        latestVersion = releaseInfo.version();
        fileName = releaseInfo.fileName();

        String currentVersion = plugin.getPluginMeta().getVersion();
        if (Objects.equals(currentVersion, latestVersion)) {
            logVersionStatus(currentVersion, latestVersion, true);
            return CompletableFuture.completedFuture(UpdateStatus.ALREADY_UP_TO_DATE);
        }

        logVersionStatus(currentVersion, latestVersion, false);
        return downloadJarAsync(releaseInfo.downloadUrl(), releaseInfo.fileName())
                .thenApply(tempFile -> moveToUpdateFolder(tempFile, releaseInfo.fileName()));
    }

    private @NotNull ReleaseInfo parseReleaseInfo(String responseBody) {
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

                long expectedLength = response.headers()
                        .firstValueAsLong("Content-Length")
                        .orElse(-1L);

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

    private @NotNull HttpRequest createGitHubApiRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Flats-Plugin-Updater")
                .GET()
                .build();
    }

    private @NotNull HttpRequest createDownloadRequest(String url) {
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

    private @NotNull UpdateStatus moveToUpdateFolder(@NotNull Path tempFile, @Nullable String targetFileName) {
        File updateFolder = plugin.getServer().getUpdateFolderFile();
        if (targetFileName == null || targetFileName.isBlank()) {
            plugin.getLogger().log(Level.SEVERE, "Could not resolve target file name");
            deleteQuietly(tempFile);
            return UpdateStatus.FAILED;
        }

        Path targetPath = updateFolder.toPath().resolve(targetFileName);
        try {
            Files.createDirectories(updateFolder.toPath());
            Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger()
                    .log(Level.INFO, () -> "Update downloaded to update folder: " + targetPath
                            + ". Will apply on next restart.");
            return UpdateStatus.SUCCESS;
        } catch (IOException e) {
            logException("Failed to move file to update folder", e);
            deleteQuietly(tempFile);
            return UpdateStatus.FAILED;
        }
    }

    private void logVersionStatus(String currentVersion, String latestVersion, boolean isUpToDate) {
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

    private void logException(String message, Throwable e) {
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
