package de.nvclas.flats.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

/**
 * Manages the process of downloading and updating a plugin by retrieving the latest release
 * from a specified GitHub API URL and handling related operations such as file downloading, moving
 * to the appropriate directory, and plugin cleanup.
 */
public class UpdateDownloader {

    private static final String UPDATE_PROCESS_ERROR = "An error occurred during the update process";
    private static final long UNKNOWN_CONTENT_LENGTH = -1L;
    private static final Gson GSON = new Gson();
    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

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
        } catch (CompletionException e) {
            Throwable cause = e.getCause() instanceof Exception exception ? exception : e;
            logException(UPDATE_PROCESS_ERROR, cause);
            return UpdateStatus.FAILED;
        } catch (Exception e) {
            logException(UPDATE_PROCESS_ERROR, e);
            return UpdateStatus.FAILED;
        }
    }

    public CompletableFuture<UpdateStatus> downloadLatestReleaseAsync() {
        return fetchLatestReleaseAsync()
                .thenCompose(this::processRelease)
                .exceptionally(e -> {
                    Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
                    logException(UPDATE_PROCESS_ERROR, cause);
                    return UpdateStatus.FAILED;
                });
    }

    private CompletableFuture<ReleaseInfo> fetchLatestReleaseAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(createGitHubApiRequest(apiUrl),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 404) {
                    return ReleaseInfo.notFound();
                }
                if (response.statusCode() != 200) {
                    throw new IOException("Failed to fetch latest release: HTTP " + response.statusCode());
                }
                return parseReleaseInfo(response.body());
            } catch (IOException e) {
                throw new CompletionException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
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
        return downloadJarAsync(releaseInfo.downloadUrl())
                .thenApply(this::moveJarToPlugins);
    }

    private @NotNull ReleaseInfo parseReleaseInfo(String responseBody) throws IOException {
        JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);
        if (jsonResponse == null) {
            throw new IOException("GitHub API returned an invalid JSON body");
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

    private CompletableFuture<Path> downloadJarAsync(String downloadUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<InputStream> response = httpClient.send(createDownloadRequest(downloadUrl),
                        HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new IOException("Failed to download file: HTTP " + response.statusCode());
                }

                String resolvedFileName = extractFileName(response, downloadUrl);
                Path tempFile = createTempDownloadFile(resolvedFileName);
                long expectedLength = response.headers()
                        .firstValueAsLong("Content-Length")
                        .orElse(UNKNOWN_CONTENT_LENGTH);
                saveDownloadedFile(response.body(), tempFile, expectedLength);
                fileName = resolvedFileName;
                return tempFile;
            } catch (IOException e) {
                throw new CompletionException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        });
    }

    private HttpRequest createGitHubApiRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
    }

    private HttpRequest createDownloadRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "Java-HttpClient")
                .GET()
                .build();
    }

    private String extractFileName(HttpResponse<InputStream> response, String url) {
        return response.headers()
                .firstValue("Content-Disposition")
                .map(header -> header.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1"))
                .orElseGet(() -> {
                    String[] parts = url.split("/");
                    return parts[parts.length - 1];
                });
    }

    private @NotNull Path createTempDownloadFile(String resolvedFileName) throws IOException {
        Path tempDir = plugin.getDataFolder().toPath().resolve("updates");
        Files.createDirectories(tempDir);
        String sanitizedName = resolvedFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return Files.createTempFile(tempDir, "download-", "-" + sanitizedName);
    }

    private void saveDownloadedFile(InputStream inputStream, Path targetFile, long expectedLength)
            throws IOException {
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
            throw new IOException("Downloaded file is incomplete. Expected " + expectedLength
                    + " bytes but got " + bytesCopied + " bytes");
        }

        plugin.getLogger().log(Level.INFO, () -> "Download completed: " + targetFile.getFileName());
    }

    private UpdateStatus moveJarToPlugins(Path tempFile) {
        Path pluginsPath = plugin.getDataFolder().toPath().getParent();
        if (pluginsPath == null || fileName == null || fileName.isBlank()) {
            plugin.getLogger().log(Level.SEVERE, "Could not resolve plugins directory or target file name");
            deleteQuietly(tempFile);
            return UpdateStatus.FAILED;
        }

        Path targetPath = pluginsPath.resolve(fileName);
        try {
            Files.createDirectories(pluginsPath);
            Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().log(Level.INFO, () -> "Moved file to plugins directory: " + targetPath);
            return UpdateStatus.SUCCESS;
        } catch (IOException e) {
            logException("Failed to move file to plugins directory", e);
            deleteQuietly(tempFile);
            return UpdateStatus.FAILED;
        }
    }

    private void deleteQuietly(Path path) {
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

        private boolean exists() {
            return version != null && fileName != null && downloadUrl != null;
        }

        private static ReleaseInfo notFound() {
            return new ReleaseInfo(null, null, null);
        }
    }
}
