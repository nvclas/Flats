package de.nvclas.flats.listeners.protection;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkitExtension;
import org.mockbukkit.mockbukkit.MockBukkitInject;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockBukkitExtension.class)
class EntityExplodeListenerTest {

    @MockBukkitInject
    private ServerMock server;

    private WorldMock world;
    private Flats plugin;
    private FlatsCache flatsCache;
    private EntityExplodeListener listener;
    private Flat testFlat;

    @BeforeEach
    void setUp() {
        world = server.addSimpleWorld("world");
        
        // Mock plugin and cache
        plugin = mock(Flats.class);
        flatsCache = mock(FlatsCache.class);
        when(plugin.getFlatsCache()).thenReturn(flatsCache);
        
        // Create test flat
        Location pos1 = new Location(world, 0, 0, 0);
        Location pos2 = new Location(world, 10, 10, 10);
        Area area = new Area("test-area", pos1, pos2);
        testFlat = new Flat("test-flat", area);
        
        listener = new EntityExplodeListener(plugin);
    }

    @Test
    @DisplayName("Should remove all blocks within flat from TNT explosion damage")
    void testTNTBlocksInFlatAreProtected() {
        // Create blocks - some in flat, some outside
        List<Block> explodingBlocks = new ArrayList<>();
        
        // Blocks inside flat (should be removed)
        Block blockInFlat1 = world.getBlockAt(5, 5, 5);
        Block blockInFlat2 = world.getBlockAt(7, 7, 7);
        Block blockInFlat3 = world.getBlockAt(2, 2, 2);
        
        // Blocks outside flat (should not be removed)
        Block blockOutsideFlat1 = world.getBlockAt(15, 15, 15);
        Block blockOutsideFlat2 = world.getBlockAt(20, 20, 20);
        
        explodingBlocks.add(blockInFlat1);
        explodingBlocks.add(blockOutsideFlat1);
        explodingBlocks.add(blockInFlat2);
        explodingBlocks.add(blockOutsideFlat2);
        explodingBlocks.add(blockInFlat3);
        
        // Mock flatsCache to return flat for blocks inside, null for blocks outside
        when(flatsCache.getFlatByLocation(blockInFlat1.getLocation())).thenReturn(testFlat);
        when(flatsCache.getFlatByLocation(blockInFlat2.getLocation())).thenReturn(testFlat);
        when(flatsCache.getFlatByLocation(blockInFlat3.getLocation())).thenReturn(testFlat);
        when(flatsCache.getFlatByLocation(blockOutsideFlat1.getLocation())).thenReturn(null);
        when(flatsCache.getFlatByLocation(blockOutsideFlat2.getLocation())).thenReturn(null);
        
        // Create TNT entity and explosion event
        TNTPrimed tnt = world.spawn(new Location(world, 5, 5, 5), TNTPrimed.class);
        EntityExplodeEvent event = new EntityExplodeEvent(tnt, new Location(world, 5, 5, 5), explodingBlocks, 0.0f);
        
        listener.onEntityExplode(event);
        
        // Verify that only blocks outside flat remain
        assertEquals(2, event.blockList().size(), "Should have exactly 2 blocks remaining");
        assertFalse(event.blockList().contains(blockInFlat1), "Block in flat should be removed");
        assertFalse(event.blockList().contains(blockInFlat2), "Block in flat should be removed");
        assertFalse(event.blockList().contains(blockInFlat3), "Block in flat should be removed");
        assertEquals(blockOutsideFlat1, event.blockList().get(0), "Block outside flat should remain");
        assertEquals(blockOutsideFlat2, event.blockList().get(1), "Block outside flat should remain");
    }
    
    @Test
    @DisplayName("Should handle empty block list gracefully")
    void testEmptyBlockList() {
        List<Block> explodingBlocks = new ArrayList<>();
        TNTPrimed tnt = world.spawn(new Location(world, 5, 5, 5), TNTPrimed.class);
        EntityExplodeEvent event = new EntityExplodeEvent(tnt, new Location(world, 5, 5, 5), explodingBlocks, 0.0f);
        
        // Should not throw exception
        listener.onEntityExplode(event);
        
        assertEquals(0, event.blockList().size(), "Empty list should remain empty");
    }
    
    @Test
    @DisplayName("Should handle case where all blocks are in flat")
    void testAllBlocksInFlat() {
        List<Block> explodingBlocks = new ArrayList<>();
        Block blockInFlat1 = world.getBlockAt(5, 5, 5);
        Block blockInFlat2 = world.getBlockAt(7, 7, 7);
        
        explodingBlocks.add(blockInFlat1);
        explodingBlocks.add(blockInFlat2);
        
        // Mock both blocks as being in flat
        when(flatsCache.getFlatByLocation(blockInFlat1.getLocation())).thenReturn(testFlat);
        when(flatsCache.getFlatByLocation(blockInFlat2.getLocation())).thenReturn(testFlat);
        
        TNTPrimed tnt = world.spawn(new Location(world, 5, 5, 5), TNTPrimed.class);
        EntityExplodeEvent event = new EntityExplodeEvent(tnt, new Location(world, 5, 5, 5), explodingBlocks, 0.0f);
        
        listener.onEntityExplode(event);
        
        assertEquals(0, event.blockList().size(), "All blocks should be removed");
    }
    
    @Test
    @DisplayName("Should handle case where no blocks are in flat")
    void testNoBlocksInFlat() {
        List<Block> explodingBlocks = new ArrayList<>();
        Block blockOutside1 = world.getBlockAt(15, 15, 15);
        Block blockOutside2 = world.getBlockAt(20, 20, 20);
        
        explodingBlocks.add(blockOutside1);
        explodingBlocks.add(blockOutside2);
        
        // Mock no blocks as being in flat
        when(flatsCache.getFlatByLocation(blockOutside1.getLocation())).thenReturn(null);
        when(flatsCache.getFlatByLocation(blockOutside2.getLocation())).thenReturn(null);
        
        TNTPrimed tnt = world.spawn(new Location(world, 5, 5, 5), TNTPrimed.class);
        EntityExplodeEvent event = new EntityExplodeEvent(tnt, new Location(world, 5, 5, 5), explodingBlocks, 0.0f);
        
        listener.onEntityExplode(event);
        
        assertEquals(2, event.blockList().size(), "No blocks should be removed");
        assertEquals(blockOutside1, event.blockList().get(0), "First block should remain");
        assertEquals(blockOutside2, event.blockList().get(1), "Second block should remain");
    }
}