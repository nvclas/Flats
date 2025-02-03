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
 * The {@code UpdateDownloader} class is responsible for managing the process of downloading,
 * updating, and replacing plugin files in a Minecraft server environment.
 * <p>
 * It fetches the latest release of the plugin from a specified GitHub API endpoint,
 * downloads the corresponding JAR file, and replaces the existing plugin JAR.
 * <p>
 * The class also provides functionality to unload the currently running plugin and delete the old JAR file.
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
     * Downloads the latest release of the plugin from a specified GitHub API endpoint.
     * The method performs several asynchronous operations, including fetching the download URL,
     * downloading the file, and moving the downloaded JAR file to the plugins directory.
     * <p>
     * If any errors occur during the process, they are logged, and the method returns a failure status.
     *
     * @return {@link UpdateStatus} indicating the result of the update operation. Possible values are:
     * - {@code UpdateStatus.SUCCESS}: The latest release was successfully downloaded and moved to the plugins directory.
     * - {@code UpdateStatus.FAILED}: An error occurred during the update process.
     * - {@code UpdateStatus.NOT_FOUND}: No valid JAR file was found in the latest release.
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
     * Unloads the current plugin and deletes its associated JAR file asynchronously.
     * This method should be run when {@link #downloadLatestRelease()} returns {@link UpdateStatus#SUCCESS}.
     * <p>
     * This method retrieves the plugin instance from the Bukkit plugin manager using the name
     * of the current plugin. If the plugin is found, it is disabled using the plugin manager.
     * After successfully disabling the plugin, the JAR file corresponding to the plugin
     * is deleted asynchronously.
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