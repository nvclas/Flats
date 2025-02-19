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

    public UpdateDownloader(JavaPlugin plugin, String apiUrl) {
        this.plugin = plugin;
        this.apiUrl = apiUrl;
    }

    /**
     * Downloads the latest release of the plugin and moves the downloaded file to the plugins directory.
     * The method fetches the latest release URL asynchronously, downloads the file if available,
     * and performs necessary file operations to place the plugin in the appropriate location.
     * Handles errors during the process and reports success, failure, or the absence of a release.
     *
     * @return an {@link UpdateStatus} indicating the result of the operation:
     *         {@code SUCCESS} if the update was downloaded and moved successfully,
     *         {@code NOT_FOUND} if no suitable release was found,
     *         or {@code FAILED} if any error occurred during the download or file operations.
     */
    public UpdateStatus downloadLatestRelease() {
        try {
            return fetchLatestReleaseUrlAsync().thenCompose(downloadUrl -> {
                if (downloadUrl.isEmpty()) {
                    return CompletableFuture.completedFuture(UpdateStatus.NOT_FOUND);
                }
                return downloadFileAsync(downloadUrl).thenApply(v -> {
                    moveJarToPlugins();
                    return UpdateStatus.SUCCESS;
                });
            }).exceptionally(e -> {
                plugin.getLogger()
                        .log(Level.SEVERE, e, () -> "An error occurred during the update process: " + e.getMessage());
                return UpdateStatus.FAILED;
            }).join();
        } catch (Exception e) {
            plugin.getLogger()
                    .log(Level.SEVERE, e, () -> "An error occurred during the update process: " + e.getMessage());
            return UpdateStatus.FAILED;
        }
    }

    /**
     * Unloads the current plugin and asynchronously deletes its jar file.
     * <p>
     * This method disables the plugin using the Bukkit Plugin Manager to ensure
     * it is no longer active. Afterward, it triggers an asynchronous operation
     * to delete the plugin's jar file from the plugins directory.
     * <p>
     * The deletion process is performed in a non-blocking manner, but any
     * potential errors during the deletion are logged.
     */
    public void unloadPluginAndDeleteJar() {
        Plugin targetPlugin = Bukkit.getPluginManager().getPlugin(plugin.getName());
        if (targetPlugin != null) {
            Bukkit.getPluginManager().disablePlugin(targetPlugin);
        }
        deleteCurrentJarAsync();
    }

    private void deleteCurrentJarAsync() {
        File currentJar = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        File pluginJar = new File(PLUGINS_DIR, currentJar.getName());
        try {
            Files.delete(pluginJar.toPath());
            plugin.getLogger().log(Level.INFO, () -> "Deleted current jar file " + pluginJar.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, () -> "Failed to delete plugin jar file: " + e.getMessage());
        }
    }

    private @NotNull CompletableFuture<String> fetchLatestReleaseUrlAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Accept", "application/vnd.github+json")
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = GSON.fromJson(response.body(), JsonObject.class);
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
                } else {
                    plugin.getLogger()
                            .log(Level.SEVERE, () -> "Failed to fetch latest release: HTTP " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                plugin.getLogger().log(Level.SEVERE, e, () -> "Error fetching latest release URL: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            return "";
        });
    }

    private CompletableFuture<Void> downloadFileAsync(String downloadUrl) {
        return CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "Java-HttpClient")
                        .GET()
                        .build();

                HttpResponse<InputStream> response = HTTP_CLIENT.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    plugin.getLogger()
                            .log(Level.SEVERE, () -> "Failed to download file: HTTP Status " + response.statusCode());
                    return;
                }

                fileName = response.headers()
                        .firstValue("Content-Disposition")
                        .map(header -> header.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1"))
                        .orElseGet(() -> {
                            String[] parts = downloadUrl.split("/");
                            return parts[parts.length - 1];
                        });

                try (InputStream inputStream = response.body(); BufferedInputStream bufferedInputStream = new BufferedInputStream(
                        inputStream); FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {

                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    long totalBytesRead = 0;

                    while ((bytesRead = bufferedInputStream.read(dataBuffer, 0, dataBuffer.length)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }

                    long expectedLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                    if (expectedLength != -1 && totalBytesRead != expectedLength) {
                        long finalTotalBytesRead = totalBytesRead;
                        plugin.getLogger()
                                .log(Level.SEVERE,
                                        () -> "Downloaded file is incomplete. Expected: " + expectedLength + " bytes, received: " + finalTotalBytesRead + " bytes.");
                    } else {
                        plugin.getLogger().log(Level.INFO, () -> "Download completed: " + fileName);
                    }
                }
            } catch (IOException | InterruptedException e) {
                plugin.getLogger().log(Level.SEVERE, e, () -> "Error downloading file: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        });
    }


    private void moveJarToPlugins() {
        Path sourcePath = Path.of(fileName);
        Path targetPath = Path.of(PLUGINS_DIR, fileName);

        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().log(Level.INFO, () -> "Moved file to plugins directory: " + targetPath);
        } catch (IOException e) {
            plugin.getLogger()
                    .log(Level.SEVERE, e, () -> "Failed to move file to plugins directory: " + e.getMessage());
        }
    }
}