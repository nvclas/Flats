package de.nvclas.flats.cache;

import de.nvclas.flats.Flats;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.MockBukkitExtension;
import org.mockbukkit.mockbukkit.MockBukkitInject;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockBukkitExtension.class)
@DisplayName("FlatsCache and Caching Tests")
class FlatsCacheTest {

    @MockBukkitInject
    private ServerMock server;
    @MockBukkitInject
    private Flats plugin;
    @MockBukkitInject
    private WorldMock world;
    @MockBukkitInject
    private PlayerMock player;

    private FlatsCache flatsCache;

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

    @Test
    @DisplayName("getFlatAtLocation waits for cache loading when cell is uncached")
    void testGetFlatAtLocationWaitsWhenUncached() {
        Area area = new Area(new Location(world, 100, 10, 100), new Location(world, 120, 20, 120), "cached_flat");
        Flat flat = new Flat("cached_flat", area);
        plugin.getFlatsStorage().saveFlat(flat);

        // Ensure cache is cold for this location by creating a query at (105, 15, 105)
        Location queryLocation = new Location(world, 105, 15, 105);

        // Querying flat at location should ensure loaded and return the flat immediately even without prior movement
        Flat result = flatsCache.getFlatAtLocation(queryLocation);

        assertNotNull(result, "Flat should be found even if cache was cold initially");
        assertEquals("cached_flat", result.getName(), "Should return correct flat name");
    }

    @Test
    @DisplayName("prefetchSurroundingGridCells loads adjacent grid cells asynchronously")
    void testPrefetchSurroundingGridCells() {
        Location center = new Location(world, 0, 64, 0);
        flatsCache.prefetchSurroundingGridCells(center, 1);

        // Grid cells -1, 0, 1 in x and z should eventually be loaded
        CompletableFuture<Void> queryFuture = flatsCache.prefetchLocation(new Location(world, 16, 64, 16));
        queryFuture.join();

        Area area = flatsCache.getAreaAtLocation(new Location(world, 16, 64, 16));
        assertNull(area, "No flat in area, but cell is safely queryable");
    }

    @Test
    @DisplayName("PlayerMoveListener prefetches on grid cell crossing without errors")
    void testPlayerMoveListenerBoundaryCross() {
        Location to = new Location(world, 20, 64, 20);
        new PlayerSimulation(player).simulatePlayerMove(to);

        Flat atTo = flatsCache.getFlatAtLocation(to);
        assertNull(atTo);
    }

    @Test
    @DisplayName("PlayerTeleportEvent prefetches surrounding cells")
    void testPlayerTeleportEventPrefetch() {
        Location targetLoc = new Location(world, 500, 64, 500);
        player.teleport(targetLoc);

        Flat result = flatsCache.getFlatAtLocation(targetLoc);
        assertNull(result);
    }
}
