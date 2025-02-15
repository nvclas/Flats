package de.nvclas.flats.commands;


import de.nvclas.flats.Flats;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.testutil.TestUtil;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.volumes.Selection;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlatsCommandTest {

    private PlayerMock player;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        MockBukkit.load(Flats.class);
        player = server.addPlayer();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }


    @ParameterizedTest
    @CsvSource({
            "flats unknown, commands.help.header",
            "flats, commands.help.header",
            "flats add testFlat, messages.nothing_selected",
            "flats remove testFlat, messages.flat_not_exist",
            "flats claim, messages.not_in_flat"
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
        TestUtil.assertEqualMessage(Flats.PREFIX + I18n.translate("messages.no_permission"), message);
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
        Selection.getSelection(player).setPos1(new Location(world,1, 1, 1));
        Selection.getSelection(player).setPos2(new Location(world,10, 10, 10));
        assertEquals(1000, Selection.getSelection(player).calculateVolume());
        player.performCommand("flats add testFlat");
        TestUtil.assertEqualMessage(Flats.PREFIX + I18n.translate("messages.flat_created", "testFlat"), player.nextMessage());
    }

    @Test
    void testRemoveCommand() {
        createValidFlat();
        player.setOp(true);
        player.performCommand("flats remove testFlat");
        TestUtil.assertEqualMessage(Flats.PREFIX + I18n.translate("messages.flat_deleted", "testFlat"), player.nextMessage());
    }

    @Test
    void testClaimCommand() {
        createValidFlat();
        player.setLocation(new Location(world, 5, 5, 5));
        player.performCommand("flats claim");
        TestUtil.assertEqualMessage(Flats.PREFIX + I18n.translate("messages.claim_success"), player.nextMessage());
    }

    /**
     * Creates a valid flat area selection for the player and registers it as a new flat
     * named {@code testFlat}. The selection is defined by two corners in the world at {@code 0, 0, 0} and {@code 10, 10, 10}.
    **/
    private void createValidFlat() {
        player.setOp(true);
        Selection.getSelection(player).setPos1(new Location(world,1, 1, 1));
        Selection.getSelection(player).setPos2(new Location(world,10, 10, 10));
        player.performCommand("flats add testFlat");
        player.nextMessage();
        player.setOp(false);

    }
}