package de.nvclas.flats.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.nvclas.flats.Flats;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class UpdateDownloader {

    private static final String API_URL = "https://api.github.com/repos/nvclas/Flats/releases/latest";
    private static final String PLUGINS_DIR = "plugins";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    private final Flats plugin;
    @Getter
    private String fileName;

    public UpdateDownloader(Flats plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<String> fetchLatestReleaseUrlAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
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
                            Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().info("Fetched latest release URL: " + asset.get("browser_download_url").getAsString()));
                            return asset.get("browser_download_url").getAsString();
                        }
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().warning("No JAR asset found in the latest release."));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().severe("Failed to fetch latest release: HTTP " + response.statusCode()));
                }
            } catch (IOException | InterruptedException e) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().severe("Error fetching latest release URL: " + e.getMessage()));
            }
            return null;
        });
    }

    public CompletableFuture<Void> downloadFileAsync(String downloadUrl) {
        return CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "Java-HttpClient")
                        .GET()
                        .build();

                HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().severe("Failed to download file: HTTP Status " + response.statusCode()));
                    return;
                }

                fileName = response.headers().firstValue("Content-Disposition")
                        .map(header -> header.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1"))
                        .orElseGet(() -> {
                            String[] parts = downloadUrl.split("/");
                            return parts[parts.length - 1];
                        });

                try (InputStream inputStream = response.body();
                     BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                     FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {

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
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().severe("Downloaded file is incomplete. Expected: " + expectedLength + " bytes, received: " + finalTotalBytesRead + " bytes."));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().info("Download completed: " + fileName));
                    }
                }
            } catch (IOException | InterruptedException e) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().severe("Error downloading file: " + e.getMessage()));
            }
        });
    }

    private void deletePreviousJar() {
        File pluginsDir = new File(PLUGINS_DIR);
        File[] jarFiles = pluginsDir.listFiles((dir, name) -> name.startsWith("Flats") && name.endsWith(".jar"));

        if (jarFiles != null) {
            for (File file : jarFiles) {
                if (file.delete()) {
                    plugin.getLogger().info("Deleted previous jar file: " + file.getName());
                } else {
                    plugin.getLogger().warning("Failed to delete file: " + file.getName());
                }
            }
        } else {
            plugin.getLogger().info("No previous jar files to delete.");
        }
    }

    public void unloadPluginAndDeleteJar() {
        Plugin targetPlugin = Bukkit.getPluginManager().getPlugin(plugin.getName());
        if (targetPlugin != null) {
            Bukkit.getPluginManager().disablePlugin(targetPlugin);
        }
        deletePreviousJar();
    }

    public void moveJarToPluginsAsync() {
        CompletableFuture.runAsync(() -> {
            Path sourcePath = Path.of(fileName);
            Path targetPath = Path.of(PLUGINS_DIR, fileName);

            try {
                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().info("Moved file to plugins directory: " + targetPath));
            } catch (IOException e) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().severe("Failed to move file to plugins directory: " + e.getMessage()));
            }
        });
    }
}