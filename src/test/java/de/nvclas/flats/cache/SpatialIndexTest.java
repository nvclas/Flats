package de.nvclas.flats.cache;

import de.nvclas.flats.volumes.Area;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SpatialIndex Tests")
class SpatialIndexTest {

    private WorldMock worldA;
    private WorldMock worldB;
    private SpatialIndex spatialIndex;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        worldA = server.addSimpleWorld("world_a");
        worldB = server.addSimpleWorld("world_b");
        spatialIndex = new SpatialIndex();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Loading a cell in world A does not mark the same (x,z) cell in world B as loaded")
    void testGridCellIsIsolatedByWorld() {
        // Load grid cell (0, 0) for world A at block coordinates (0, 0, 0)
        spatialIndex.setAreas("world_a", 0, 0, List.of());

        Location locationInWorldA = new Location(worldA, 0, 64, 0);
        Location locationInWorldB = new Location(worldB, 0, 64, 0);

        assertTrue(spatialIndex.isLoaded(locationInWorldA),
                "Grid cell (0,0) in world_a should be loaded after setAreas");
        assertFalse(spatialIndex.isLoaded(locationInWorldB),
                "Grid cell (0,0) in world_b must NOT be considered loaded just because world_a's cell was loaded");
    }

    @Test
    @DisplayName("Areas in world A are not returned when querying from world B at the same block coordinates")
    void testGetFlatNameAtLocationIsIsolatedByWorld() {
        // Register an area in world A at coordinates (0,0,0)-(10,10,10)
        Area areaInA = new Area(new Location(worldA, 0, 0, 0), new Location(worldA, 10, 10, 10), "flat_a");
        spatialIndex.setAreas("world_a", 0, 0, List.of(areaInA));

        // Query the same block coordinates in world A — must find the flat
        Location inWorldA = new Location(worldA, 5, 5, 5);
        assertEquals("flat_a", spatialIndex.getFlatNameAtLocation(inWorldA),
                "getFlatNameAtLocation in world_a should return 'flat_a'");

        // Query the exact same block coordinates in world B — must NOT find flat_a
        Location inWorldB = new Location(worldB, 5, 5, 5);
        assertNull(spatialIndex.getFlatNameAtLocation(inWorldB),
                "getFlatNameAtLocation in world_b must not return flat_a (different world)");
    }

    @Test
    @DisplayName("Two worlds can each have independent areas at the same (x,z) coordinates")
    void testIndependentAreasAcrossWorlds() {
        Area areaInA = new Area(new Location(worldA, 0, 0, 0), new Location(worldA, 10, 10, 10), "flat_a");
        Area areaInB = new Area(new Location(worldB, 0, 0, 0), new Location(worldB, 10, 10, 10), "flat_b");

        spatialIndex.setAreas("world_a", 0, 0, List.of(areaInA));
        spatialIndex.setAreas("world_b", 0, 0, List.of(areaInB));

        Location inWorldA = new Location(worldA, 5, 5, 5);
        Location inWorldB = new Location(worldB, 5, 5, 5);

        assertEquals("flat_a", spatialIndex.getFlatNameAtLocation(inWorldA),
                "world_a should return flat_a");
        assertEquals("flat_b", spatialIndex.getFlatNameAtLocation(inWorldB),
                "world_b should return flat_b");
    }
}
