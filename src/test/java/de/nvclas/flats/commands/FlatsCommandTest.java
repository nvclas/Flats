package de.nvclas.flats.commands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.testutil.TestUtil;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.volumes.Flat;
import de.nvclas.flats.volumes.Selection;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FlatsCommandTest {

    private static final String TEST_FLAT_NAME = "testFlat";

    private ServerMock server;
    private Flats plugin;
    private PlayerMock player;
    private PlayerMock target;
    private WorldMock world;
    private FlatsCache flatsCache;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Flats.class);
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        target = server.addPlayer();
        flatsCache = plugin.getFlatsCache();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        if (plugin.getDataFolder().exists() && !plugin.getDataFolder().delete()) {
            fail("Could not delete plugin data folder.");
        }
    }

    @ParameterizedTest
    @CsvSource({
            "flats unknown, help.header",
            "flats, help.header",
            "flats add testFlat, error.nothing_selected",
            "flats remove testFlat, error.flat_not_exist",
            "flats claim, error.not_in_flat"
    })
    void testCommandFailures(String command, String messageKey) {
        executeCommandAsOp(command);
        verifyMessageEquals(I18n.translate(messageKey));
    }

    @Test
    void testSelectCommandNoPermission() {
        executeCommand("flats select");
        verifyMessageEquals(I18n.translate("error.no_permission"));
    }

    @Test
    void testSelectCommand() {
        executeCommandAsOp("flats select");
        assertTrue(player.getInventory().contains(SelectionItem.getItem()),
                "Player should receive the selection item.");
    }

    @Test
    void testAddCommand() {
        setupValidSelection();
        executeCommandAsOp("flats add " + TEST_FLAT_NAME);
        verifyMessageEquals(I18n.translate("add.success", TEST_FLAT_NAME));
        assertTrue(flatsCache.existsFlat(TEST_FLAT_NAME), "Flat should exist after being added.");
    }

    @Test
    void testRemoveCommand() {
        createValidFlat();
        executeCommandAsOp("flats remove " + TEST_FLAT_NAME);
        verifyMessageEquals(I18n.translate("remove.success", TEST_FLAT_NAME));
        assertFalse(flatsCache.existsFlat(TEST_FLAT_NAME), "Flat should not exist after removal.");
    }

    @Test
    void testClaimCommand() {
        Flat createdFlat = createValidFlat();
        player.setLocation(new Location(world, 5, 5, 5));
        executeCommand("flats claim");
        verifyMessageEquals(I18n.translate("claim.success"));
        assertTrue(createdFlat.isOwner(player), "Player should be the owner of the claimed flat.");
    }

    @Test
    void testSaveWorldWithDeletedWorld() {
        createValidFlat();
        server.removeWorld(world);
        assertDoesNotThrow(() -> flatsCache.saveAll(),
                "Save operation should not throw an exception even if the world is deleted.");
    }

    @Test
    void testTrustCommandWithOnlineTarget() {
        testClaimCommand();
        executeCommand("flats trust " + target.getName());
        verifyMessageEquals(I18n.translate("trust.success", target.getName()));
        Flat flat = flatsCache.getExistingFlat(TEST_FLAT_NAME);
        assertTrue(flat.isTrusted(target), "Target player should be trusted in the flat.");
    }

    @Test
    void testTrustCommandWithOfflineTarget() {
        target.kick();
        testTrustCommandWithOnlineTarget();
    }

    @Test
    void testUntrustCommandWithOnlineTarget() {
        testTrustCommandWithOnlineTarget();
        executeCommand("flats untrust " + target.getName());
        verifyMessageEquals(I18n.translate("untrust.success", target.getName()));
        Flat flat = flatsCache.getExistingFlat(TEST_FLAT_NAME);
        assertFalse(flat.isTrusted(target), "Target player should no longer be trusted in the flat.");
    }

    @Test
    void testUntrustCommandWithOfflineTarget() {
        target.kick();
        testUntrustCommandWithOnlineTarget();
    }

    /**
     * Helper method to execute a command as an operator.
     */
    private void executeCommandAsOp(String command) {
        player.setOp(true);
        executeCommand(command);
        player.setOp(false);
    }

    /**
     * Helper method to execute a command and ignore operator status.
     */
    private void executeCommand(String command) {
        assertTrue(player.performCommand(command), "Command execution should succeed: " + command);
    }

    /**
     * Helper method to verify the player's next message.
     */
    private void verifyMessageEquals(String expectedMessageKey, Object... formatArgs) {
        String expectedMessage = Flats.PREFIX + I18n.translate(expectedMessageKey, formatArgs);
        String actualMessage = player.nextMessage();
        assertNotNull(actualMessage, "Player should receive a message.");
        TestUtil.assertEqualMessage(expectedMessage, actualMessage);
    }

    /**
     * Sets up a valid selection for the player.
     */
    private void setupValidSelection() {
        Selection selection = Selection.getSelection(player);
        selection.setPos1(new Location(world, 1, 1, 1));
        selection.setPos2(new Location(world, 10, 10, 10));
        assertEquals(1000, selection.calculateVolume(), "Selection volume should be 1000.");
    }

    /**
     * Creates a valid flat area selection for the player and registers it as a new flat
     * named {@code testFlat}.
     *
     * @return The created {@link Flat}, or {@code null} if creation failed.
     */
    private @NotNull Flat createValidFlat() {
        setupValidSelection();
        executeCommandAsOp("flats add " + TEST_FLAT_NAME);
        verifyMessageEquals("add.success", TEST_FLAT_NAME);
        return flatsCache.getExistingFlat(TEST_FLAT_NAME);
    }
}