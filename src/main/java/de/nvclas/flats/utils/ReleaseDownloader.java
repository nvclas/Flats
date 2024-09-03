package de.nvclas.flats.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.nvclas.flats.Flats;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ReleaseDownloader {

    private static final String API_URL = "https://api.github.com/repos/nvclas/Flats/releases/latest";
    private static final String PLUGINS_DIR = "plugins";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    private final Flats plugin;

    public ReleaseDownloader(Flats plugin) {
        this.plugin = plugin;
    }

    public String fetchLatestReleaseUrl() throws IOException, InterruptedException {
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
                    plugin.getLogger().info("Fetched latest release URL: " + asset.get("browser_download_url").getAsString());
                    return asset.get("browser_download_url").getAsString();
                }
            }
            plugin.getLogger().warning("No JAR asset found in the latest release.");
        } else {
            plugin.getLogger().severe("Failed to fetch latest release: HTTP " + response.statusCode());
        }
        return null;
    }

    public void downloadFile(String downloadUrl, String fileName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("Accept", "application/vnd.github+json") // Richtiger Header für GitHub API
                .header("User-Agent", "Java-HttpClient") // Einfache User-Agent Einstellung
                .GET()
                .build();

        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // Überprüfen des HTTP-Statuscodes
        if (response.statusCode() != 200) {
            plugin.getLogger().severe("Fehler beim Herunterladen: HTTP-Status " + response.statusCode());
            return;
        }

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
            fileOutputStream.flush();

            long expectedLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (expectedLength != -1 && totalBytesRead != expectedLength) {
                plugin.getLogger().severe("Die heruntergeladene Datei ist unvollständig. Erwartet: " + expectedLength + " Bytes, erhalten: " + totalBytesRead + " Bytes.");
            } else {
                plugin.getLogger().info("Download abgeschlossen: " + fileName);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Herunterladen der Datei: " + e.getMessage());
            throw e;
        }
    }

    public void deletePreviousJar() {
        File pluginsDir = new File(PLUGINS_DIR);
        File[] jarFiles = pluginsDir.listFiles((dir, name) -> name.startsWith("Flats") && name.endsWith(".jar"));

        if (jarFiles != null && jarFiles.length > 0) {
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

    public void moveJarToPlugins(String fileName) throws IOException {
        Path sourcePath = Path.of(fileName);
        Path targetPath = Path.of(PLUGINS_DIR, fileName);

        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Moved file to plugins directory: " + targetPath);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to move file to plugins directory: " + e.getMessage());
            throw e;
        }
    }
}