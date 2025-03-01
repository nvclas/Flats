package de.nvclas.flats.commands;


import de.nvclas.flats.Flats;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.testutil.TestUtil;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.volumes.Flat;
import de.nvclas.flats.volumes.Selection;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class FlatsCommandTest {

    private static final String TEST_FLAT_NAME = "testFlat";

    private ServerMock server;
    private Flats plugin;
    private PlayerMock player;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Flats.class);
        player = server.addPlayer();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }


    @ParameterizedTest
    @CsvSource({
            "flats unknown, help.header",
            "flats, help.header",
            "flats add testFlat, nothing_selected",
            "flats remove testFlat, flat_not_exist",
            "flats claim, not_in_flat"
    })
    void testCommandFailures(String command, String messageKey) {
        player.setOp(true);
        player.performCommand(command);
        String message = player.nextMessage();
        assertNotNull(message);
        TestUtil.assertEqualMessage(Flats.PREFIX + I18n.translate(messageKey), message);
    }

    @Test
    void testSelectCommandNoPermission() {
        player.performCommand("flats select");
        String message = player.nextMessage();
        assertNotNull(message);
        TestUtil.assertEqualMessage(Flats.PREFIX + I18n.translate("error.no_permission"), message);
    }

    @Test
    void testSelectCommand() {
        player.setOp(true);
        player.performCommand("flats select");
        assertTrue(player.getInventory().contains(SelectionItem.getItem()));
    }

    @Test
    void testAddCommand() {
        player.setOp(true);
        Selection.getSelection(player).setPos1(new Location(world, 1, 1, 1));
        Selection.getSelection(player).setPos2(new Location(world, 10, 10, 10));
        assertEquals(1000, Selection.getSelection(player).calculateVolume());
        player.performCommand("flats add testFlat");
        TestUtil.assertEqualMessage(Flats.PREFIX + I18n.translate("add.success", TEST_FLAT_NAME),
                player.nextMessage());
        assertTrue(plugin.getFlatsManager().existsFlat(TEST_FLAT_NAME));
    }

    @Test
    void testRemoveCommand() {
        createValidFlat();
        player.setOp(true);
        player.performCommand("flats remove testFlat");
        TestUtil.assertEqualMessage(Flats.PREFIX + I18n.translate("remove.success", TEST_FLAT_NAME),
                player.nextMessage());
    }

    @Test
    void testClaimCommand() {
        Flat created = createValidFlat();
        player.setLocation(new Location(world, 5, 5, 5));
        player.performCommand("flats claim");
        TestUtil.assertEqualMessage(Flats.PREFIX + I18n.translate("claim.success"), player.nextMessage());
        assertTrue(created.isOwner(player));
    }

    @Test
    void testSaveWorldWithDeletedWorld() {
        createValidFlat();
        server.removeWorld(world);
        assertDoesNotThrow(() -> plugin.getFlatsManager().saveAll());
    }

    /**
     * Creates a valid flat area selection for the player and registers it as a new flat
     * named {@code testFlat}. The selection is defined by two corners in the world at {@code 0, 0, 0} and {@code 10, 10, 10}.
     **/
    private Flat createValidFlat() {
        player.setOp(true);
        Selection.getSelection(player).setPos1(new Location(world, 1, 1, 1));
        Selection.getSelection(player).setPos2(new Location(world, 10, 10, 10));
        player.performCommand("flats add testFlat");
        player.nextMessage();
        player.setOp(false);
        assertTrue(plugin.getFlatsManager().existsFlat(TEST_FLAT_NAME));
        return plugin.getFlatsManager().getFlat(TEST_FLAT_NAME);
    }
}