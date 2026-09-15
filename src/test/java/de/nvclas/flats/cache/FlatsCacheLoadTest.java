package de.nvclas.flats.cache;

import de.nvclas.flats.Flats;
import de.nvclas.flats.events.FlatEnteredOrLeftEvent;
import de.nvclas.flats.listeners.PlayerMoveListener;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.MockBukkitExtension;
import org.mockbukkit.mockbukkit.MockBukkitInject;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class for load and benchmark scenarios verifying {@link FlatsCache},
 * {@link SpatialIndex}, and {@link PlayerMoveListener}.
 * <p>
 * These tests act as regression guards to catch performance bottlenecks, O(n) regressions,
 * or concurrency issues under high load.
 */
@ExtendWith(MockBukkitExtension.class)
@DisplayName("FlatsCache Load & Benchmark Tests")
class FlatsCacheLoadTest {

    @MockBukkitInject
    private ServerMock server;
    @MockBukkitInject
    private Flats plugin;
    @MockBukkitInject
    private WorldMock world;

    private FlatsCache flatsCache;

    /**
     * Converts nanoseconds to milliseconds.
     */
    private static double toMillis(long nanos) {
        return nanos / (double) TimeUnit.MILLISECONDS.toNanos(1);
    }

    @BeforeEach
    void setUp() {
        flatsCache = plugin.getFlatsCache();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        if (plugin.getDataFolder().exists() && !plugin.getDataFolder().delete()) {
            fail("Could not delete plugin data folder.");
        }
    }

    /**
     * Creates a standard test area with a offset multiplier to keep flats separated.
     */
    private Area createTestArea(int index, int spacing, String name) {
        int baseX = index * spacing;
        return new Area(
                new Location(world, baseX, 0, 0),
                new Location(world, baseX + 10, 10, 10),
                name
        );
    }

    /**
     * Benchmark tests for {@link FlatsCache}, {@link SpatialIndex}, and {@link PlayerMoveListener}.
     */
    @Nested
    @DisplayName("Benchmark Tests")
    class BenchmarkTests {

        @Test
        @Timeout(30)
        @DisplayName("Bulk flat creation and cached lookup throughput")
        void benchmarkBulkCreateAndLookup() {
            int flatCount = 1_000;
            int spacing = 20;

            long createStart = System.nanoTime();
            for (int i = 0; i < flatCount; i++) {
                String name = "load_flat_" + i;
                flatsCache.create(name, createTestArea(i, spacing, name));
            }
            double createMsPerOp = toMillis(System.nanoTime() - createStart) / flatCount;

            long lookupStart = System.nanoTime();
            for (int i = 0; i < flatCount; i++) {
                Flat flat = flatsCache.getFlat("load_flat_" + i);
                assertNotNull(flat, "Flat load_flat_" + i + " should exist.");
            }
            double lookupMsPerOp = toMillis(System.nanoTime() - lookupStart) / flatCount;

            assertTrue(createMsPerOp < 20.0, "Creation degraded: " + createMsPerOp + " ms/op");
            assertTrue(lookupMsPerOp < 1.0, "Lookup should be near-instant, was " + lookupMsPerOp + " ms/op");
        }

        @Test
        @Timeout(30)
        @DisplayName("Concurrent cold-cache prefetches from multiple players")
        void benchmarkConcurrentColdCachePrefetchBurst() {
            int playerCount = 150;
            int spacing = 200;

            for (int i = 0; i < playerCount; i++) {
                String name = "burst_flat_" + i;
                plugin.getFlatsStorage().saveFlat(new Flat(name, createTestArea(i, spacing, name)));
            }

            long start = System.nanoTime();
            List<CompletableFuture<Void>> futures = new ArrayList<>(playerCount);
            for (int i = 0; i < playerCount; i++) {
                Location loc = new Location(world, i * spacing + 5, 5, 5);
                futures.add(flatsCache.prefetchLocation(loc));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            long elapsed = System.nanoTime() - start;

            for (int i = 0; i < playerCount; i++) {
                Location loc = new Location(world, i * spacing + 5, 5, 5);
                assertTrue(flatsCache.isLocationLoaded(loc), "Cell for player " + i + " should be loaded.");
                Flat flat = flatsCache.getFlatAtLocation(loc);
                assertNotNull(flat, "Flat for player " + i + " should be resolvable.");
                assertEquals("burst_flat_" + i, flat.getName());
            }

            assertTrue(toMillis(elapsed) < 15_000, "Cold-cache burst took too long: " + toMillis(elapsed) + " ms");
        }

        @Test
        @Timeout(30)
        @DisplayName("removeFlat scales with flat size, not total cache size")
        void benchmarkRemoveFlatScalability() {
            int unrelatedCellCount = 2_000;

            List<CompletableFuture<Void>> loadFutures = new ArrayList<>(unrelatedCellCount + 1);
            loadFutures.add(flatsCache.prefetchGridCell("world", 0, 0));
            for (int i = 0; i < unrelatedCellCount; i++) {
                loadFutures.add(flatsCache.prefetchGridCell("world", 10_000 + i, 10_000 + i));
            }
            CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0])).join();

            Area area = new Area(new Location(world, 0, 0, 0), new Location(world, 10, 10, 10), "removable_flat");
            Flat flat = new Flat("removable_flat", area);
            flatsCache.create("removable_flat", area);

            long start = System.nanoTime();
            flatsCache.delete(flat);
            long elapsed = System.nanoTime() - start;

            assertFalse(flatsCache.existsFlat("removable_flat"), "Flat should be deleted.");
            assertTrue(toMillis(elapsed) < 50.0, "removeFlat took too long: " + toMillis(elapsed) + " ms");
        }

        @Test
        @Timeout(60)
        @DisplayName("PlayerMoveListener handling under swarm movement")
        void benchmarkPlayerMoveListenerSwarm() {
            int playerCount = 40;
            int movesPerPlayer = 25;
            int stepSize = SpatialIndex.GRID_SIZE + 1;

            AtomicInteger flatEvents = new AtomicInteger();
            server.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onFlatEvent(FlatEnteredOrLeftEvent event) {
                    flatEvents.incrementAndGet();
                }
            }, plugin);

            for (int i = 0; i < movesPerPlayer; i++) {
                String name = "path_flat_" + i;
                flatsCache.create(name, createTestArea(i, stepSize, name));
            }

            List<PlayerMock> players = new ArrayList<>(playerCount);
            for (int i = 0; i < playerCount; i++) {
                players.add(server.addPlayer());
            }

            long start = System.nanoTime();
            for (PlayerMock player : players) {
                for (int step = 0; step < movesPerPlayer; step++) {
                    Location to = new Location(world, step * stepSize + 2, 5, 2);
                    new PlayerSimulation(player).simulatePlayerMove(to);
                }
            }
            long elapsed = System.nanoTime() - start;

            int totalMoves = playerCount * movesPerPlayer;
            double msPerMove = toMillis(elapsed) / totalMoves;

            assertTrue(flatEvents.get() > 0, "Move events should have fired.");
            assertTrue(msPerMove < 5.0, "Per-move handling degraded: " + msPerMove + " ms/move");
        }
    }

    /**
     * Capacity & storage load tests for {@link FlatsCache}, {@link SpatialIndex}, and {@link PlayerMoveListener}.
     */
    @Nested
    @DisplayName("Capacity & Storage Load Tests")
    class CapacityLoadTests {

        @Test
        @Timeout(30)
        @DisplayName("Exceeding cache capacity maintains fallback correctness")
        void loadTestExceedingCacheCapacityStaysCorrect() {
            int flatCount = 1_500;
            int spacing = 20;

            for (int i = 0; i < flatCount; i++) {
                String name = "capacity_flat_" + i;
                flatsCache.create(name, createTestArea(i, spacing, name));
            }

            for (int i = flatCount - 10; i < flatCount; i++) {
                assertNotNull(flatsCache.getFlat("capacity_flat_" + i), "Hot flat should be retrievable.");
            }
            for (int i = 0; i < 10; i++) {
                assertNotNull(flatsCache.getFlat("capacity_flat_" + i), "Evicted flat should fall back to storage.");
            }

            assertEquals(flatCount, flatsCache.getTotalFlatsCount(), "All flats should remain persisted.");
        }
    }
}
