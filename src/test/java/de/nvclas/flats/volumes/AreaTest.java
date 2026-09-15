package de.nvclas.flats.volumes;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaTest {

    private WorldMock world;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        world = server.addSimpleWorld("test_world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void isWithinDistance() {
        Area area = new Area(new Location(world, 0, 0, 0), new Location(world, 300, 10, 10), "test_flat");

        assertTrue(area.isWithinDistance(new Location(world, 150, 5, 5), 100));
        assertTrue(area.isWithinDistance(new Location(world, 350, 5, 5), 50));
        assertFalse(area.isWithinDistance(new Location(world, 401, 5, 5), 100));
    }

    @Test
    void getWorldReturnsLoadedWorld() {
        Area area = new Area(new Location(world, 0, 0, 0), new Location(world, 10, 10, 10), "test_flat");

        assertEquals(world, area.getWorld());
    }

    @Test
    void getWorldReturnsNullWhenWorldNotLoaded() {
        Area.Bounds bounds = new Area.Bounds(0, 10, 0, 10, 0, 10);
        Area area = Area.fromRawData("unloaded_world", bounds, "test_flat");

        assertNull(area.getWorld());
    }

    @Test
    void getEdgesReturnsTwelveEdges() {
        Area area = new Area(new Location(world, 0, 0, 0), new Location(world, 10, 4, 6), "test_flat");

        List<Area.Edge> edges = area.getEdges();

        assertEquals(12, edges.size());
    }

    @Test
    void getEdgesWrapsFullBlockVolume() {
        Area area = new Area(new Location(world, 0, 0, 0), new Location(world, 10, 4, 6), "test_flat");

        List<Area.Edge> edges = area.getEdges();

        boolean hasMinCorner = edges.stream()
                .flatMap(edge -> java.util.stream.Stream.of(edge.start(), edge.end()))
                .anyMatch(loc -> loc.getX() == 0 && loc.getY() == 0 && loc.getZ() == 0);
        boolean hasMaxCorner = edges.stream()
                .flatMap(edge -> java.util.stream.Stream.of(edge.start(), edge.end()))
                .anyMatch(loc -> loc.getX() == 11 && loc.getY() == 5 && loc.getZ() == 7);

        assertTrue(hasMinCorner, "Expected an edge touching the min corner (0,0,0)");
        assertTrue(hasMaxCorner, "Expected an edge touching the max corner one block past (10,4,6), i.e. (11,5,7)");
    }

    @Test
    void getEdgesHaveExpectedLengthsPerAxis() {
        // Distinct sizes per axis so edges group unambiguously by length.
        Area area = new Area(new Location(world, 0, 0, 0), new Location(world, 10, 4, 6), "test_flat");

        double sizeX = 11; // maxX - minX + 1
        double sizeY = 5;  // maxY - minY + 1
        double sizeZ = 7;  // maxZ - minZ + 1

        Map<Double, Long> lengthCounts = area.getEdges().stream()
                .map(edge -> edge.start().distance(edge.end()))
                .collect(Collectors.groupingBy(length -> Math.round(length * 1000.0) / 1000.0,
                        Collectors.counting()));

        assertEquals(3, lengthCounts.size(), "Expected exactly 3 distinct edge lengths (one per axis)");
        assertEquals(4L, lengthCounts.get(sizeX), "Expected 4 edges running along the X axis");
        assertEquals(4L, lengthCounts.get(sizeY), "Expected 4 edges running along the Y axis");
        assertEquals(4L, lengthCounts.get(sizeZ), "Expected 4 edges running along the Z axis");
    }

    @Test
    void getEdgesReturnsEmptyListWhenWorldNotLoaded() {
        Area.Bounds bounds = new Area.Bounds(0, 10, 0, 10, 0, 10);
        Area area = Area.fromRawData("unloaded_world", bounds, "test_flat");

        assertTrue(area.getEdges().isEmpty());
    }

    @Test
    void allEdgeEndpointsAreDistinct() {
        Area area = new Area(new Location(world, 0, 0, 0), new Location(world, 10, 4, 6), "test_flat");

        long distinctCorners = area.getEdges().stream()
                .flatMap(edge -> java.util.stream.Stream.of(edge.start(), edge.end()))
                .map(loc -> List.of(loc.getX(), loc.getY(), loc.getZ()))
                .distinct()
                .count();

        assertEquals(8, distinctCorners, "A cuboid has exactly 8 distinct corners");
    }
}
