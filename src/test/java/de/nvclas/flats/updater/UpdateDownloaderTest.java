package de.nvclas.flats.updater;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.nvclas.flats.Flats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.MockBukkitExtension;
import org.mockbukkit.mockbukkit.MockBukkitInject;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockBukkitExtension.class)
@DisplayName("UpdateDownloader Tests")
class UpdateDownloaderTest {

    @MockBukkitInject
    private ServerMock server;
    @MockBukkitInject
    private Flats plugin;

    private HttpServer httpServer;
    private Path movedJar;

    @AfterEach
    void tearDown() throws IOException {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (movedJar != null) {
            Files.deleteIfExists(movedJar);
        }
        MockBukkit.unmock();
        deleteRecursively(plugin.getDataFolder().toPath());
    }

    @Test
    @DisplayName("Returns success and moves the downloaded jar into the plugins directory")
    void downloadLatestReleaseSuccess() throws Exception {
        byte[] jarBytes = "fake-jar".getBytes(StandardCharsets.UTF_8);
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/releases/latest", exchange -> respondJson(exchange, """
                {
                  "tag_name": "v9.9.9",
                  "assets": [
                    {
                      "name": "Flats-9.9.9.jar",
                      "browser_download_url": "%s/downloads/Flats-9.9.9.jar"
                    }
                  ]
                }
                """.formatted(baseUrl())));
        httpServer.createContext("/downloads/Flats-9.9.9.jar", exchange -> {
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"Flats-9.9.9.jar\"");
            exchange.sendResponseHeaders(200, jarBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jarBytes);
            }
        });
        httpServer.start();

        UpdateDownloader downloader = new UpdateDownloader(plugin, baseUrl() + "/releases/latest");

        UpdateStatus status = downloader.downloadLatestRelease();

        movedJar = plugin.getDataFolder().toPath().getParent().resolve("Flats-9.9.9.jar");
        assertEquals(UpdateStatus.SUCCESS, status);
        assertTrue(Files.exists(movedJar), "Downloaded jar should be moved to the plugins directory");
        assertEquals(jarBytes.length, Files.size(movedJar));
    }

    @Test
    @DisplayName("Returns not found when the release has no jar asset")
    void releaseWithoutJarAssetReturnsNotFound() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/releases/latest", exchange -> respondJson(exchange, """
                {
                  "tag_name": "v2.1.0",
                  "assets": []
                }
                """));
        httpServer.start();

        UpdateDownloader downloader = new UpdateDownloader(plugin, baseUrl() + "/releases/latest");

        UpdateStatus status = downloader.downloadLatestRelease();

        assertEquals(UpdateStatus.NOT_FOUND, status);
    }

    @Test
    @DisplayName("Returns already up to date when the latest version matches the current plugin version")
    void alreadyUpToDate() throws Exception {
        String currentVersion = plugin.getPluginMeta().getVersion();
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/releases/latest", exchange -> respondJson(exchange, """
                {
                  "tag_name": "v%s",
                  "assets": [
                    {
                      "name": "Flats-%s.jar",
                      "browser_download_url": "%s/downloads/Flats-%s.jar"
                    }
                  ]
                }
                """.formatted(currentVersion, currentVersion, baseUrl(), currentVersion)));
        httpServer.start();

        UpdateDownloader downloader = new UpdateDownloader(plugin, baseUrl() + "/releases/latest");

        UpdateStatus status = downloader.downloadLatestRelease();

        assertEquals(UpdateStatus.ALREADY_UP_TO_DATE, status);
    }

    @Test
    @DisplayName("Returns failed when the jar download endpoint errors")
    void failedJarDownloadReturnsFailed() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/releases/latest", exchange -> respondJson(exchange, """
                {
                  "tag_name": "v9.9.9",
                  "assets": [
                    {
                      "name": "Flats-9.9.9.jar",
                      "browser_download_url": "%s/downloads/Flats-9.9.9.jar"
                    }
                  ]
                }
                """.formatted(baseUrl())));
        httpServer.createContext("/downloads/Flats-9.9.9.jar", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        httpServer.start();

        UpdateDownloader downloader = new UpdateDownloader(plugin, baseUrl() + "/releases/latest");

        UpdateStatus status = downloader.downloadLatestRelease();

        movedJar = plugin.getDataFolder().toPath().getParent().resolve("Flats-9.9.9.jar");
        assertEquals(UpdateStatus.FAILED, status);
        assertFalse(Files.exists(movedJar), "No target jar should be moved on failed download");
    }

    private String baseUrl() {
        return "http://localhost:" + httpServer.getAddress().getPort();
    }

    private void respondJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void deleteRecursively(Path path) {
        try {
            if (Files.notExists(path)) {
                return;
            }
            try (Stream<Path> paths = Files.walk(path)) {
                paths.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(current -> {
                            try {
                                Files.deleteIfExists(current);
                            } catch (IOException _) {
                                fail("Could not delete test path: " + current);
                            }
                        });
            }
        } catch (IOException _) {
            fail("Could not walk test path: " + path);
        }
    }
}
