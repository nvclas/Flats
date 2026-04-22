package de.nvclas.flats.volumes;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void getAllOuterBlocksSmall() {
        Location pos1 = new Location(world, 0, 0, 0);
        Location pos2 = new Location(world, 1, 1, 1);
        Area area = new Area(pos1, pos2, "test_flat");

        List<Block> outerBlocks = area.getAllOuterBlocks();

        // (0,0,0) to (1,1,1)
        // x: 0 to 1, y: 0 to 1, z: 0 to 1
        // Total blocks = 2*2*2 = 8 blocks. All are on the boundary.
        assertEquals(8, outerBlocks.size());

        assertTrue(outerBlocks.contains(world.getBlockAt(0, 0, 0)));
        assertTrue(outerBlocks.contains(world.getBlockAt(1, 1, 1)));
        assertTrue(outerBlocks.contains(world.getBlockAt(0, 0, 1)));
        assertTrue(outerBlocks.contains(world.getBlockAt(0, 1, 0)));
        assertTrue(outerBlocks.contains(world.getBlockAt(1, 0, 0)));
        assertTrue(outerBlocks.contains(world.getBlockAt(1, 1, 0)));
        assertTrue(outerBlocks.contains(world.getBlockAt(1, 0, 1)));
        assertTrue(outerBlocks.contains(world.getBlockAt(0, 1, 1)));
    }

    @Test
    void getAllOuterBlocksLarger() {
        Location pos1 = new Location(world, 0, 0, 0);
        Location pos2 = new Location(world, 2, 2, 2);
        Area area = new Area(pos1, pos2, "test_flat");

        List<Block> outerBlocks = area.getAllOuterBlocks();

        // (0,0,0) to (2,2,2)
        // Total blocks = 3*3*3 = 27
        // Inner blocks = (1,1,1) -> 1 block
        // Boundary blocks = 27 - 1 = 26
        assertEquals(26, outerBlocks.size());
    }

    @Test
    void intersectsHorizontalRange() {
        Area area = new Area(new Location(world, 0, 0, 0), new Location(world, 300, 10, 10), "test_flat");

        assertTrue(area.intersectsHorizontalRange(new Location(world, 150, 5, 5), 100));
        assertTrue(area.intersectsHorizontalRange(new Location(world, 350, 5, 5), 50));
        assertFalse(area.intersectsHorizontalRange(new Location(world, 401, 5, 5), 100));
    }
}
