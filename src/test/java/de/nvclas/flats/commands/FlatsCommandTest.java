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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/**
 * Unit tests for testing command functionality in the Flats plugin.
 * <p>
 * Test cases include validation of permissions, command success and failure cases,
 * handling of edge cases, and integration scenarios.
 * <p>
 * The class is structured into nested test categories:
 * <ul>
 * <li>General Command Tests: Validates generic command behavior and error handling.</li>
 * <li>Select Command Tests: Focuses on the "select" command.</li>
 * <li>Add/Remove Command Tests: Validates flat creation and deletion commands.</li>
 * <li>Claim/Unclaim Command Tests: Verifies the claiming and unclaiming functionality of flats.</li>
 * </ul>
 * <p>
 * Helper methods are used throughout to abstract repetitive tasks like setting up a valid
 * flat selection, performing commands, and asserting expected outcomes.
 */
@DisplayName("Flats Command Tests")
class FlatsCommandTest {

    // Constants
    private static final String TEST_FLAT_NAME = "testFlat";
    private static final int SELECTION_MIN_COORD = 1;
    private static final int SELECTION_MAX_COORD = 10;
    private static final int SELECTION_VOLUME = 1000;
    private static final int DEFAULT_FLAT_INTERIOR_COORD = 5;
    private static final int FAR_AWAY_COORD = 1000;

    // Test fixtures
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

    /**
     * Helper method to create a flat and set the current player as its owner.
     */
    private void createAndClaimFlat() {
        Flat flat = createValidFlat();
        flat.setOwner(player);
        placePlayerInFlat();
    }

    /**
     * Helper method to execute a command as an operator.
     *
     * @param command The command to execute
     */
    private void executeCommandAsOp(String command) {
        player.setOp(true);
        executeCommand(command);
        player.setOp(false);
    }

    /**
     * Helper method to execute a command and verify it succeeds.
     *
     * @param command The command to execute
     */
    private void executeCommand(String command) {
        assertTrue(player.performCommand(command), "Command execution should succeed: " + command);
    }

    /**
     * Helper method to verify the player's next message matches the expected translation.
     *
     * @param expectedMessageKey The translation key for the expected message
     * @param formatArgs         Optional format arguments for the translation
     */
    private void verifyMessageEquals(String expectedMessageKey, Object... formatArgs) {
        String expectedMessage = Flats.PREFIX + I18n.translate(expectedMessageKey, formatArgs);
        String actualMessage = player.nextMessage();
        assertNotNull(actualMessage, "Player should receive a message.");
        TestUtil.assertEqualMessage(expectedMessage, actualMessage);
    }

    /**
     * Sets up a valid selection for the player using the predefined coordinates.
     */
    private void setupValidSelection() {
        Selection selection = Selection.getSelection(player);
        selection.setPos1(new Location(world, SELECTION_MIN_COORD, SELECTION_MIN_COORD, SELECTION_MIN_COORD));
        selection.setPos2(new Location(world, SELECTION_MAX_COORD, SELECTION_MAX_COORD, SELECTION_MAX_COORD));
        assertEquals(SELECTION_VOLUME,
                selection.calculateVolume(),
                "Selection volume should be " + SELECTION_VOLUME + ".");
    }

    /**
     * Creates a valid flat area selection for the player and registers it as a new flat
     * with the predefined test name.
     *
     * @return The created {@link Flat}
     */
    private @NotNull Flat createValidFlat() {
        setupValidSelection();
        executeCommandAsOp("flats add " + TEST_FLAT_NAME);
        verifyMessageEquals("add.success", TEST_FLAT_NAME);
        return flatsCache.getExistingFlat(TEST_FLAT_NAME);
    }

    /**
     * Places the player at a location inside the test flat.
     */
    private void placePlayerInFlat() {
        player.setLocation(new Location(world,
                DEFAULT_FLAT_INTERIOR_COORD,
                DEFAULT_FLAT_INTERIOR_COORD,
                DEFAULT_FLAT_INTERIOR_COORD));
    }

    /**
     * Places the player at a location far away from any flat.
     */
    private void placePlayerFarFromFlats() {
        player.setLocation(new Location(world, FAR_AWAY_COORD, DEFAULT_FLAT_INTERIOR_COORD, FAR_AWAY_COORD));
    }

    /**
     * Tests for general command behavior and error cases.
     */
    @Nested
    @DisplayName("General Command Tests")
    class GeneralCommandTests {

        @ParameterizedTest(name = "Command \"{0}\" should show message key \"{1}\"")
        @CsvSource({"flats unknown, help.header", "flats, help.header", "flats add testFlat, error.nothing_selected", "flats remove testFlat, error.flat_not_exist", "flats claim, error.not_in_flat"})
        @DisplayName("Command failure cases")
        void testCommandFailures(String command, String messageKey) {
            executeCommandAsOp(command);
            verifyMessageEquals(messageKey);
        }
    }

    /**
     * Tests for the "select" command.
     */
    @Nested
    @DisplayName("Select Command Tests")
    class SelectCommandTests {

        @Test
        @DisplayName("Player without permission cannot use select command")
        void testSelectCommandNoPermission() {
            executeCommand("flats select");
            verifyMessageEquals("error.no_permission");
        }

        @Test
        @DisplayName("Player with permission receives selection item")
        void testSelectCommand() {
            executeCommandAsOp("flats select");
            assertTrue(player.getInventory().contains(SelectionItem.getItem()),
                    "Player should receive the selection item.");
        }
    }

    /**
     * Tests for the "add" and "remove" commands.
     */
    @Nested
    @DisplayName("Add/Remove Command Tests")
    class AddRemoveCommandTests {

        @Test
        @DisplayName("Add command creates a new flat")
        void testAddCommand() {
            setupValidSelection();
            executeCommandAsOp("flats add " + TEST_FLAT_NAME);
            verifyMessageEquals("add.success", TEST_FLAT_NAME);
            assertTrue(flatsCache.existsFlat(TEST_FLAT_NAME), "Flat should exist after being added.");
        }

        @Test
        @DisplayName("Creating a flat that intersects an existing flat returns error message")
        void testFlatIntersectionError() {
            createValidFlat();
            Selection selection = Selection.getSelection(player);
            selection.setPos1(new Location(world, SELECTION_MIN_COORD, SELECTION_MIN_COORD, SELECTION_MIN_COORD));
            selection.setPos2(new Location(world,
                    SELECTION_MAX_COORD + 5,
                    SELECTION_MAX_COORD + 5,
                    SELECTION_MAX_COORD + 5));

            executeCommandAsOp("flats add newFlat");
            verifyMessageEquals("error.flat_intersect");
            assertFalse(flatsCache.existsFlat("newFlat"),
                    "Flat should not be created when intersecting with existing flat.");

        }

        @Test
        @DisplayName("Remove command deletes an existing flat")
        void testRemoveCommand() {
            createValidFlat();
            executeCommandAsOp("flats remove " + TEST_FLAT_NAME);
            verifyMessageEquals("remove.success", TEST_FLAT_NAME);
            assertFalse(flatsCache.existsFlat(TEST_FLAT_NAME), "Flat should not exist after removal.");
        }

        @Test
        @DisplayName("Save operation works even with deleted world")
        void testSaveWorldWithDeletedWorld() {
            createValidFlat();
            server.removeWorld(world);
            assertDoesNotThrow(() -> flatsCache.saveAll(),
                    "Save operation should not throw an exception even if the world is deleted.");
        }
    }

    /**
     * Tests for the "claim" and "unclaim" commands.
     */
    @Nested
    @DisplayName("Claim/Unclaim Command Tests")
    class ClaimUnclaimCommandTests {

        @Test
        @DisplayName("Claim command sets player as owner")
        void testClaimCommand() {
            Flat createdFlat = createValidFlat();
            placePlayerInFlat();
            executeCommand("flats claim");
            verifyMessageEquals("claim.success");
            assertTrue(createdFlat.isOwner(player), "Player should be the owner of the claimed flat.");
        }

        @Test
        @DisplayName("Unclaim command removes ownership")
        void testUnclaimCommand() {
            Flat flat = createValidFlat();
            flat.setOwner(player);
            placePlayerInFlat();

            executeCommand("flats unclaim");
            verifyMessageEquals("unclaim.success");
            assertFalse(flat.hasOwner(), "Flat should no longer have an owner after unclaiming.");
        }

        @Test
        @DisplayName("Cannot unclaim flat owned by another player")
        void testUnclaimCommandWithoutOwnership() {
            Flat flat = createValidFlat();
            flat.setOwner(target);
            placePlayerInFlat();

            executeCommand("flats unclaim");
            verifyMessageEquals("error.not_your_flat");
            assertTrue(flat.isOwner(target), "Flat should still have owner after unauthorized unclaim attempt.");
        }
    }

    /**
     * Tests for the "trust" and "untrust" commands.
     */
    @Nested
    @DisplayName("Trust/Untrust Command Tests")
    class TrustUntrustCommandTests {

        @Test
        @DisplayName("Trust command adds online player to trusted list")
        void testTrustCommandWithOnlineTarget() {
            createAndClaimFlat();

            executeCommand("flats trust " + target.getName());
            verifyMessageEquals("trust.success", target.getName());

            Flat flat = flatsCache.getExistingFlat(TEST_FLAT_NAME);
            assertTrue(flat.isTrusted(target), "Target player should be trusted in the flat.");
        }

        @Test
        @DisplayName("Trust command works with offline player")
        void testTrustCommandWithOfflineTarget() {
            createAndClaimFlat();
            target.kick();

            executeCommand("flats trust " + target.getName());
            verifyMessageEquals("trust.success", target.getName());

            Flat flat = flatsCache.getExistingFlat(TEST_FLAT_NAME);
            assertTrue(flat.isTrusted(target), "Offline target player should be trusted in the flat.");
        }

        @Test
        @DisplayName("Untrust command removes online player from trusted list")
        void testUntrustCommandWithOnlineTarget() {
            createAndClaimFlat();
            executeCommand("flats trust " + target.getName());
            verifyMessageEquals("trust.success", target.getName());

            executeCommand("flats untrust " + target.getName());
            verifyMessageEquals("untrust.success", target.getName());
            Flat flat = flatsCache.getExistingFlat(TEST_FLAT_NAME);
            assertFalse(flat.isTrusted(target), "Target player should no longer be trusted after untrusting.");
        }

        @Test
        @DisplayName("Untrust command works with offline player")
        void testUntrustCommandWithOfflineTarget() {
            createAndClaimFlat();
            executeCommand("flats trust " + target.getName());
            verifyMessageEquals("trust.success", target.getName());
            target.kick();

            executeCommand("flats untrust " + target.getName());
            verifyMessageEquals("untrust.success", target.getName());
            Flat flat = flatsCache.getExistingFlat(TEST_FLAT_NAME);
            assertFalse(flat.isTrusted(target), "Offline target player should no longer be trusted after untrusting.");
        }
    }

    /**
     * Tests for the "info", "list", and "show" commands.
     */
    @Nested
    @DisplayName("Info/List/Show Command Tests")
    class InfoListShowCommandTests {

        @Test
        @DisplayName("Info command shows flat details when player is in a flat")
        void testInfoCommandWhenInFlat() {
            createAndClaimFlat();

            executeCommand("flats info");
            verifyMessageEquals("info.flat", TEST_FLAT_NAME);
            verifyMessageEquals("info.owner", player.getName());
        }

        @Test
        @DisplayName("Info command shows error when player is not in a flat")
        void testInfoCommandWhenNotInFlat() {
            placePlayerFarFromFlats();

            executeCommand("flats info");
            verifyMessageEquals("error.not_in_flat");
        }

        @Test
        @DisplayName("List command shows all flats")
        void testListCommand() {
            createValidFlat();

            executeCommandAsOp("flats list");
            verifyMessageEquals("list.title");
            verifyMessageEquals("info.flat", TEST_FLAT_NAME);
            verifyMessageEquals("info.unoccupied");
        }

        @Test
        @DisplayName("Show command highlights nearby flats")
        void testShowCommand() {
            createValidFlat();
            placePlayerInFlat();

            executeCommandAsOp("flats show");
            verifyMessageEquals("show.success", 10);
            // Visual assertion isn't applicable in tests but confirm no errors occur.
        }

        @Test
        @DisplayName("Show command works when no flats are nearby")
        void testShowCommandNoNearbyFlats() {
            placePlayerFarFromFlats();

            executeCommandAsOp("flats show");
            verifyMessageEquals("show.success", 10);
            // Visual assertion isn't applicable in tests but confirm no errors occur.
        }
    }
}
