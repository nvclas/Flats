package de.nvclas.flats.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages the process of downloading and updating a plugin by retrieving the latest release
 * from a specified GitHub API URL and handling related operations such as file downloading, moving
 * to the appropriate directory, and plugin cleanup.
 */
public class UpdateDownloader {

    private static final String PLUGINS_DIR = "plugins";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private final JavaPlugin plugin;
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
    }

    /**
     * Downloads the latest release of the plugin and moves the downloaded file to the plugins directory.
     * The method fetches the latest release URL asynchronously, compares versions to check if an update is needed,
     * downloads the file if a newer version is available, and performs necessary file operations to place
     * the plugin in the appropriate location.
     * Handles errors during the process and reports success, failure, or the absence of a release.
     *
     * @return an {@link UpdateStatus} indicating the result of the operation:
     * {@code SUCCESS} if the update was downloaded and moved successfully,
     * {@code NOT_FOUND} if no suitable release was found,
     * {@code ALREADY_UP_TO_DATE} if the current version is already the latest,
     * or {@code FAILED} if any error occurred during the download or file operations.
     */
    public UpdateStatus downloadLatestRelease() {
        try {
            return executeUpdateProcess().join();
        } catch (Exception e) {
            logException("An error occurred during the update process", e);
            return UpdateStatus.FAILED;
        }
    }

    /**
     * Executes the update process by fetching the latest release URL, checking if an update is needed,
     * downloading the file, and moving it to the plugins directory.
     *
     * @return A CompletableFuture containing the UpdateStatus
     */
    private CompletableFuture<UpdateStatus> executeUpdateProcess() {
        return fetchLatestReleaseUrlAsync()
                .thenCompose(this::processDownloadUrl)
                .exceptionally(this::handleUpdateException);
    }

    /**
     * Processes the download URL by checking if it exists, comparing versions,
     * and downloading the file if needed.
     *
     * @param downloadUrl The URL to download the file from
     * @return A CompletableFuture containing the UpdateStatus
     */
    private CompletableFuture<UpdateStatus> processDownloadUrl(String downloadUrl) {
        if (downloadUrl.isEmpty()) {
            return CompletableFuture.completedFuture(UpdateStatus.NOT_FOUND);
        }

        String currentVersion = plugin.getPluginMeta().getVersion();
        if (isCurrentVersionUpToDate(currentVersion, latestVersion)) {
            logVersionStatus(currentVersion, latestVersion, true);
            return CompletableFuture.completedFuture(UpdateStatus.ALREADY_UP_TO_DATE);
        }

        logVersionStatus(currentVersion, latestVersion, false);
        return downloadAndMoveFile(downloadUrl);
    }

    /**
     * Logs the version status, indicating whether the current version is up to date.
     *
     * @param currentVersion The current version of the plugin
     * @param latestVersion  The latest version available
     * @param isUpToDate     Whether the current version is up to date
     */
    private void logVersionStatus(String currentVersion, String latestVersion, boolean isUpToDate) {
        if (isUpToDate) {
            plugin.getLogger().log(Level.INFO,
                    () -> "Current version " + currentVersion + " is already up to date with latest version " + latestVersion);
        } else {
            plugin.getLogger().log(Level.INFO,
                    () -> "Updating from version " + currentVersion + " to " + latestVersion);
        }
    }

    /**
     * Downloads the file from the given URL and moves it to the plugins directory.
     *
     * @param downloadUrl The URL to download the file from
     * @return A CompletableFuture containing the UpdateStatus
     */
    private CompletableFuture<UpdateStatus> downloadAndMoveFile(String downloadUrl) {
        return downloadFileAsync(downloadUrl).thenApply(v -> {
            logCurrentJarInfo();
            return moveJarToPlugins() ? UpdateStatus.SUCCESS : UpdateStatus.FAILED;
        });
    }

    /**
     * Logs information about the current JAR file.
     */
    private void logCurrentJarInfo() {
        File currentJar = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        String currentJarName = currentJar.getName();
        plugin.getLogger().log(Level.INFO, () -> "Current jar file: " + currentJarName);
    }

    /**
     * Handles exceptions that occur during the update process.
     *
     * @param e The exception that occurred
     * @return UpdateStatus.FAILED
     */
    private UpdateStatus handleUpdateException(Throwable e) {
        plugin.getLogger().log(Level.SEVERE, e, () -> "An error occurred during the update process: " + e.getMessage());
        return UpdateStatus.FAILED;
    }

    /**
     * Compares the current version with the latest version to determine if an update is needed.
     *
     * @param currentVersion The current version of the plugin
     * @param latestVersion  The latest version available
     * @return true if the current version is up to date, false otherwise
     */
    private boolean isCurrentVersionUpToDate(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) {
            return false;
        }

        // Simply check if the versions are equal
        return currentVersion.equals(latestVersion);
    }


    /**
     * Unloads the current plugin and deletes its jar file.
     * <p>
     * This method disables the plugin using the Bukkit Plugin Manager to ensure
     * it is no longer active. It then attempts to delete the plugin's jar file
     * from the plugins directory.
     * <p>
     * Since the plugin is still running when this method is called, the actual
     * deletion will typically happen after the server restarts. This method
     * marks the file for deletion on JVM exit.
     */
    public void unloadPluginAndDeleteJar() {
        Plugin targetPlugin = Bukkit.getPluginManager().getPlugin(plugin.getName());
        if (targetPlugin != null) {
            Bukkit.getPluginManager().disablePlugin(targetPlugin);
        }
        markCurrentJarForDeletion();
    }

    private void markCurrentJarForDeletion() {
        File currentJar = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        File newJar = new File(currentJar.getPath() + "_DELETE");
        if (!currentJar.renameTo(newJar)) {
            plugin.getLogger().warning("Failed to rename current jar file");
        }
        newJar.deleteOnExit();
    }

    /**
     * Fetches the latest release URL asynchronously from the GitHub API.
     *
     * @return A CompletableFuture containing the download URL for the latest release JAR file,
     *         or an empty string if no suitable release was found or an error occurred.
     */
    private @NotNull CompletableFuture<String> fetchLatestReleaseUrlAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = createGitHubApiRequest(apiUrl);
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return extractDownloadUrlFromResponse(response.body());
                } else {
                    logHttpError("Failed to fetch latest release", response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                logException("Error fetching latest release URL", e);
                Thread.currentThread().interrupt();
            }
            return "";
        });
    }

    /**
     * Creates an HTTP request for GitHub API with appropriate headers.
     *
     * @param url The GitHub API URL to request
     * @return The configured HttpRequest
     */
    private HttpRequest createGitHubApiRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
    }

    /**
     * Extracts the download URL from the GitHub API response.
     *
     * @param responseBody The JSON response body from the GitHub API
     * @return The download URL for the JAR file, or an empty string if none was found
     */
    private String extractDownloadUrlFromResponse(String responseBody) {
        JsonObject jsonResponse = GSON.fromJson(responseBody, JsonObject.class);

        // Extract version from tag name
        extractLatestVersion(jsonResponse);

        // Find JAR asset and extract download URL
        return findJarDownloadUrl(jsonResponse);
    }

    /**
     * Extracts the latest version from the GitHub release JSON response.
     *
     * @param jsonResponse The parsed JSON response from GitHub API
     */
    private void extractLatestVersion(JsonObject jsonResponse) {
        if (jsonResponse.has("tag_name")) {
            String tagName = jsonResponse.get("tag_name").getAsString();
            // Remove 'v' prefix if present
            latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            plugin.getLogger().log(Level.INFO, () -> "Latest version: " + latestVersion);
        }
    }

    /**
     * Finds the JAR download URL from the GitHub release assets.
     *
     * @param jsonResponse The parsed JSON response from GitHub API
     * @return The download URL for the JAR file, or an empty string if none was found
     */
    private String findJarDownloadUrl(JsonObject jsonResponse) {
        JsonArray assets = jsonResponse.getAsJsonArray("assets");

        for (int i = 0; i < assets.size(); i++) {
            JsonObject asset = assets.get(i).getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (name.endsWith(".jar")) {
                String downloadUrl = asset.get("browser_download_url").getAsString();
                plugin.getLogger().log(Level.INFO, () -> "Fetched latest release URL: " + downloadUrl);
                return downloadUrl;
            }
        }

        plugin.getLogger().log(Level.WARNING, () -> "No JAR asset found in the latest release.");
        return "";
    }

    /**
     * Logs an HTTP error with the given message and status code.
     *
     * @param message    The error message
     * @param statusCode The HTTP status code
     */
    private void logHttpError(String message, int statusCode) {
        plugin.getLogger().log(Level.SEVERE, () -> message + ": HTTP " + statusCode);
    }

    /**
     * Logs an exception with the given message.
     *
     * @param message The error message
     * @param e       The exception that occurred
     */
    private void logException(String message, Exception e) {
        plugin.getLogger().log(Level.SEVERE, e, () -> message + ": " + e.getMessage());
    }

    /**
     * Downloads a file asynchronously from the given URL.
     *
     * @param downloadUrl The URL to download the file from
     * @return A CompletableFuture that completes when the download is finished
     */
    private CompletableFuture<Void> downloadFileAsync(String downloadUrl) {
        return CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = createDownloadRequest(downloadUrl);
                HttpResponse<InputStream> response = HTTP_CLIENT.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    logHttpError("Failed to download file", response.statusCode());
                    return;
                }

                fileName = extractFileName(response, downloadUrl);
                saveDownloadedFile(response.body(), fileName);

            } catch (IOException | InterruptedException e) {
                logException("Error downloading file", e);
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Creates an HTTP request for downloading a file.
     *
     * @param url The URL to download from
     * @return The configured HttpRequest
     */
    private HttpRequest createDownloadRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Java-HttpClient")
                .GET()
                .build();
    }

    /**
     * Extracts the filename from the HTTP response headers or from the URL.
     *
     * @param response The HTTP response
     * @param url      The download URL (used as fallback)
     * @return The extracted filename
     */
    private String extractFileName(HttpResponse<InputStream> response, String url) {
        return response.headers()
                .firstValue("Content-Disposition")
                .map(header -> header.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1"))
                .orElseGet(() -> {
                    String[] parts = url.split("/");
                    return parts[parts.length - 1];
                });
    }

    /**
     * Saves the downloaded file to disk.
     *
     * @param inputStream The input stream containing the file data
     * @param fileName    The name to save the file as
     */
    private void saveDownloadedFile(InputStream inputStream, String fileName) {
        try (
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                FileOutputStream fileOutputStream = new FileOutputStream(fileName)
        ) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = bufferedInputStream.read(dataBuffer, 0, dataBuffer.length)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            verifyDownloadComplete(fileName, totalBytesRead);
        } catch (IOException e) {
            logException("Error saving downloaded file", e);
        }
    }

    /**
     * Verifies that the download completed successfully by checking the file size.
     *
     * @param fileName       The name of the downloaded file
     * @param totalBytesRead The total number of bytes read during download
     */
    private void verifyDownloadComplete(String fileName, long totalBytesRead) {
        File downloadedFile = new File(fileName);
        long fileSize = downloadedFile.length();

        if (fileSize != totalBytesRead) {
            plugin.getLogger().log(Level.SEVERE,
                    () -> "Downloaded file is incomplete. Expected: " + totalBytesRead +
                            " bytes, actual file size: " + fileSize + " bytes.");
        } else {
            plugin.getLogger().log(Level.INFO, () -> "Download completed: " + fileName);
        }
    }


    /**
     * Moves the downloaded JAR file to the plugins directory.
     *
     * @return true if the file was successfully moved, false otherwise
     */
    private boolean moveJarToPlugins() {
        Path sourcePath = Path.of(fileName);
        Path targetPath = Path.of(PLUGINS_DIR, fileName);

        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().log(Level.INFO, () -> "Moved file to plugins directory: " + targetPath);
            return true;
        } catch (IOException e) {
            plugin.getLogger()
                    .log(Level.SEVERE, e, () -> "Failed to move file to plugins directory: " + e.getMessage());
            return false;
        }
    }
}
