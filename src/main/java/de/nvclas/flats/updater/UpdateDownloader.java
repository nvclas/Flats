package de.nvclas.flats.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages downloading plugin updates from GitHub and placing them into the server's update directory.
 */
public class UpdateDownloader {

    private static final String UPDATE_PROCESS_ERROR = "An error occurred during the update process";
    private static final long UNKNOWN_CONTENT_LENGTH = -1L;
    private static final Gson GSON = new Gson();
    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "(?i)filename\\*?=(?:UTF-8''|\"?)([^\";]+)\"?"
    );

    private final JavaPlugin plugin;
    private final HttpClient httpClient;

    @Getter
    @Setter
    private String apiUrl;

    @Getter
    private String fileName;

    @Getter
    private String latestVersion;

    public UpdateDownloader(JavaPlugin plugin, String apiUrl) {
        this.plugin = plugin;
        this.apiUrl = apiUrl;
        this.httpClient = DEFAULT_HTTP_CLIENT;
    }

    /**
     * Downloads the latest release of the plugin and moves the downloaded file to the plugins directory.
     *
     * @return the resulting {@link UpdateStatus}.
     */
    public UpdateStatus downloadLatestRelease() {
        try {
            return downloadLatestReleaseAsync().join();
        } catch (Exception e) {
            logException(UPDATE_PROCESS_ERROR, e);
            return UpdateStatus.FAILED;
        }
    }

    public CompletableFuture<UpdateStatus> downloadLatestReleaseAsync() {
        return fetchLatestReleaseAsync().thenCompose(this::processRelease).exceptionally(e -> {
            Throwable cause = (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
            logException(UPDATE_PROCESS_ERROR, cause);
            return UpdateStatus.FAILED;
        });
    }

    private CompletableFuture<ReleaseInfo> fetchLatestReleaseAsync() {
        HttpRequest request = createGitHubApiRequest(apiUrl);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            int statusCode = response.statusCode();
            if (statusCode == 404) {
                return ReleaseInfo.notFound();
            }
            if (statusCode == 403) {
                plugin.getLogger().log(Level.WARNING, "GitHub API rate limit exceeded or forbidden access.");
                return ReleaseInfo.notFound();
            }
            if (statusCode != 200) {
                throw new CompletionException(new IOException("Failed to fetch latest release: HTTP " + statusCode));
            }
            return parseReleaseInfo(response.body());
        });
    }

    private CompletableFuture<UpdateStatus> processRelease(@NotNull ReleaseInfo releaseInfo) {
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
        return downloadJarAsync(releaseInfo.downloadUrl()).thenApply(
                tempFile -> moveToUpdateFolder(tempFile, releaseInfo.fileName()));
    }

    private @NotNull ReleaseInfo parseReleaseInfo(String responseBody) {
        JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);
        if (jsonResponse == null) {
            throw new CompletionException(new IOException("GitHub API returned an invalid JSON body"));
        }

        String version = extractLatestVersion(jsonResponse);
        JsonArray assets = jsonResponse.getAsJsonArray("assets");
        if (version == null || assets == null) {
            return ReleaseInfo.notFound();
        }

        for (int i = 0; i < assets.size(); i++) {
            JsonObject asset = assets.get(i).getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (name.endsWith(".jar")) {
                String downloadUrl = asset.get("browser_download_url").getAsString();
                plugin.getLogger().log(Level.INFO, () -> "Fetched latest release URL: " + downloadUrl);
                return new ReleaseInfo(version, name, downloadUrl);
            }
        }

        plugin.getLogger().log(Level.WARNING, () -> "No JAR asset found in the latest release.");
        return ReleaseInfo.notFound();
    }

    private @Nullable String extractLatestVersion(JsonObject jsonResponse) {
        if (!jsonResponse.has("tag_name")) {
            return null;
        }

        String tagName = jsonResponse.get("tag_name").getAsString();
        String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        plugin.getLogger().log(Level.INFO, () -> "Latest version: " + version);
        return version;
    }

    private void logVersionStatus(String currentVersion, String latestVersion, boolean isUpToDate) {
        if (isUpToDate) {
            plugin.getLogger().log(Level.INFO, () -> "Current version " + currentVersion + " is already up to date");
        } else {
            plugin.getLogger()
                    .log(Level.INFO, () -> "Updating from version " + currentVersion + " to " + latestVersion);
        }
    }

    private CompletableFuture<Path> downloadJarAsync(@NotNull String downloadUrl) {
        HttpRequest request = createDownloadRequest(downloadUrl);
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new CompletionException(
                        new IOException("Failed to download file: HTTP " + response.statusCode()));
            }

            try {
                String resolvedFileName = extractFileName(response, downloadUrl);
                Path tempFile = createTempDownloadFile(resolvedFileName);
                long expectedLength = response.headers()
                        .firstValueAsLong("Content-Length")
                        .orElse(UNKNOWN_CONTENT_LENGTH);

                saveDownloadedFile(response.body(), tempFile, expectedLength);
                return tempFile;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private HttpRequest createGitHubApiRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Flats-Plugin-Updater")
                .GET()
                .build();
    }

    private HttpRequest createDownloadRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "Flats-Plugin-Updater")
                .GET()
                .build();
    }

    private String extractFileName(@NotNull HttpResponse<?> response, @NotNull String defaultUrl) {
        return response.headers().firstValue("Content-Disposition").map(header -> {
            Matcher matcher = FILENAME_PATTERN.matcher(header);
            return matcher.find() ? matcher.group(1).trim() : null;
        }).filter(name -> !name.isBlank()).orElseGet(() -> {
            int lastSlash = defaultUrl.lastIndexOf('/');
            return lastSlash != -1 ? defaultUrl.substring(lastSlash + 1) : defaultUrl;
        });
    }

    private @NotNull Path createTempDownloadFile(String resolvedFileName) throws IOException {
        Path tempDir = plugin.getDataFolder().toPath().resolve("updates");
        Files.createDirectories(tempDir);
        String sanitizedName = resolvedFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return Files.createTempFile(tempDir, "download-", "-" + sanitizedName);
    }

    private void saveDownloadedFile(InputStream inputStream, Path targetFile, long expectedLength) throws IOException {
        long bytesCopied;
        try (InputStream stream = inputStream) {
            bytesCopied = Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(targetFile);
            throw e;
        }

        if (bytesCopied <= 0) {
            Files.deleteIfExists(targetFile);
            throw new IOException("Downloaded file is empty");
        }
        if (expectedLength != UNKNOWN_CONTENT_LENGTH && expectedLength != bytesCopied) {
            Files.deleteIfExists(targetFile);
            throw new IOException(
                    "Downloaded file is incomplete. Expected " + expectedLength + " bytes but got " + bytesCopied
                            + " bytes");
        }

        plugin.getLogger().log(Level.INFO, () -> "Download completed: " + targetFile.getFileName());
    }

    private UpdateStatus moveToUpdateFolder(@NotNull Path tempFile, @Nullable String targetFileName) {
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
}
