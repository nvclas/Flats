package de.nvclas.flats.commands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.FlatsCommand;
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

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class for verifying the functionality of the {@link  FlatsCommand} and sub commands.
 * <p>
 * This class contains unit tests to ensure that the commands provided by the plugin
 * behave as expected in various scenarios, including valid selections, claiming flats,
 * and executing commands with appropriate permissions.
 * <p>
 * It includes setup and teardown methods, helper methods for command execution,
 * and utilities for validating flat-related operations.
 */
@DisplayName("Flats Command Tests")
class FlatsCommandTest {

    // Constants
    private static final int SELECTION_VOLUME = 1000;
    private static final int FLAT_SIZE = 10;
    private static final int MAX_Y_COORD = 100;
    private static final int FAR_AWAY_COORD = 1000;

    // Test fixtures
    private ServerMock server;
    private Flats plugin;
    private PlayerMock player;
    private PlayerMock target;
    private WorldMock world;
    private FlatsCache flatsCache;
    private Random random;

    // Per-test random values
    private String testFlatName;
    private int selectionMinX;
    private int selectionMinY;
    private int selectionMinZ;
    private int selectionMaxX;
    private int selectionMaxY;
    private int selectionMaxZ;
    private int flatInteriorX;
    private int flatInteriorY;
    private int flatInteriorZ;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Flats.class);
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        target = server.addPlayer();
        flatsCache = plugin.getFlatsCache();

        random = new Random();
        randomizeTestFlatValues();
    }

    /**
     * Randomizes all test flat values including name, coordinates, and interior points.
     * This centralizes all randomization logic in one place.
     */
    private void randomizeTestFlatValues() {
        testFlatName = "testFlat_" + UUID.randomUUID().toString().substring(0, 8);

        selectionMinX = random.nextInt(1000);
        selectionMinY = random.nextInt(MAX_Y_COORD - FLAT_SIZE);
        selectionMinZ = random.nextInt(1000);

        selectionMaxX = selectionMinX + FLAT_SIZE - 1;
        selectionMaxY = selectionMinY + FLAT_SIZE - 1;
        selectionMaxZ = selectionMinZ + FLAT_SIZE - 1;

        flatInteriorX = selectionMinX + (FLAT_SIZE / 2);
        flatInteriorY = selectionMinY + (FLAT_SIZE / 2);
        flatInteriorZ = selectionMinZ + (FLAT_SIZE / 2);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        if (plugin.getDataFolder().exists() && !plugin.getDataFolder().delete()) {
            fail("Could not delete plugin data folder.");
        }
    }

    /**
     * Creates a valid {@link Flat} instance, assigns the current player as the owner,
     * and moves the player into the created flat.
     * <p>
     * This method ensures that the created flat meets all validity constraints
     * and is properly initialized before assigning ownership and placing the player.
     * <p>
     * Intended for use in tests that require a player to be set up in an owned flat.
     */
    private Flat createAndClaimFlat() {
        Flat flat = createValidFlat();
        flat.setOwner(player);
        placePlayerInFlat();
        return flat;
    }

    /**
     * Executes a specified command as an operator by temporarily granting operator privileges
     * to the current player during the command execution.
     *
     * @param command the command to be executed as an operator; must not be {@code null}.
     */
    private void executeCommandAsOp(String command) {
        player.setOp(true);
        executeCommand(command);
        player.setOp(false);
    }

    /**
     * Executes a player command and verifies that it succeeds.
     *
     * <p>Typically used to simulate a player issuing a command and validating its behavior within
     * the test environment.
     *
     * @param command the command to be executed, including any arguments,
     *                as a {@code String}.
     */
    private void executeCommand(String command) {
        assertTrue(player.performCommand(command), "Command execution should succeed: " + command);
    }

    /**
     * Verifies that the next message received by the player matches the expected message.
     *
     * @param expectedMessageKey the translation key for the expected message.
     * @param formatArgs         optional arguments to format the expected message.
     */
    private void verifyMessageEquals(String expectedMessageKey, Object... formatArgs) {
        String expectedMessage = Flats.PREFIX + I18n.translate(expectedMessageKey, formatArgs);
        String actualMessage = player.nextMessage();
        assertNotNull(actualMessage, "Player should receive a message.");
        TestUtil.assertEqualMessage(expectedMessage, actualMessage);
    }

    /**
     * Sets up and configures a valid selection for the player by defining its two corner points.
     * <p>
     * Ensures the selection's volume matches the predefined expected value.
     */
    private void setupValidSelection() {
        Selection selection = Selection.getSelection(player);
        selection.setPos1(new Location(world, selectionMinX, selectionMinY, selectionMinZ));
        selection.setPos2(new Location(world, selectionMaxX, selectionMaxY, selectionMaxZ));
        assertEquals(SELECTION_VOLUME,
                selection.calculateVolume(),
                "Selection volume should be " + SELECTION_VOLUME + ".");
    }

    /**
     * Creates and registers a valid flat with randomized properties and ensures the operation is successful.
     * <p>
     * This method sets up a valid selection, executes the necessary commands to create the flat, and verifies
     * the success of the operation.
     *
     * @return The created {@link Flat}, guaranteed to be valid and existing in the flats cache.
     */
    private @NotNull Flat createValidFlat() {
        randomizeTestFlatValues();
        setupValidSelection();
        executeCommandAsOp("flats add " + testFlatName);
        verifyMessageEquals("add.success", testFlatName);
        return flatsCache.getExistingFlat(testFlatName);
    }

    /**
     * Places the player at the preset coordinates of the flat's interior.
     *
     * <p>
     * This method is used to position the player inside the designated area of a flat.
     * It is typically invoked during commands or operations that require the player
     * to interact with a specific flat's environment.
     *
     * <p>
     * The method assumes that the flat's interior coordinates are already predefined
     * and assigns them to the player's location in the given world.
     */
    private void placePlayerInFlat() {
        player.setLocation(new Location(world,
                flatInteriorX,
                flatInteriorY,
                flatInteriorZ));
    }

    /**
     * Places the player at a far-away location, outside the vicinity of any flats.
     * <p>
     * This method is primarily used in test scenarios to ensure the player is not within any flat's boundaries.
     */
    private void placePlayerFarFromFlats() {
        player.setLocation(new Location(world, FAR_AWAY_COORD, flatInteriorY, FAR_AWAY_COORD));
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
        void commandFailures(String command, String messageKey) {
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
        void selectCommandNoPermission() {
            executeCommand("flats select");
            verifyMessageEquals("error.no_permission");
        }

        @Test
        @DisplayName("Player with permission receives selection item")
        void selectCommand() {
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
        void addCommand() {
            setupValidSelection();
            executeCommandAsOp("flats add " + testFlatName);
            verifyMessageEquals("add.success", testFlatName);
            assertTrue(flatsCache.existsFlat(testFlatName), "Flat should exist after being added.");
        }

        @Test
        @DisplayName("Creating a flat that intersects an existing flat returns error message")
        void flatIntersectionError() {
            createValidFlat();
            Selection selection = Selection.getSelection(player);
            // Create a selection that overlaps with the existing flat
            selection.setPos1(new Location(world, selectionMinX, selectionMinY, selectionMinZ));
            selection.setPos2(new Location(world,
                    selectionMaxX + 5,
                    selectionMaxY + 5,
                    selectionMaxZ + 5));

            executeCommandAsOp("flats add newFlat");
            verifyMessageEquals("error.flat_intersect");
            assertFalse(flatsCache.existsFlat("newFlat"),
                    "Flat should not be created when intersecting with existing flat.");
        }

        @Test
        @DisplayName("Remove command deletes an existing flat")
        void removeCommand() {
            createValidFlat();
            executeCommandAsOp("flats remove " + testFlatName);
            verifyMessageEquals("remove.success", testFlatName);
            assertFalse(flatsCache.existsFlat(testFlatName), "Flat should not exist after removal.");
        }

        @Test
        @DisplayName("Save operation works even with deleted world")
        void saveWorldWithDeletedWorld() {
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
        void claimCommand() {
            Flat createdFlat = createValidFlat();
            placePlayerInFlat();
            executeCommand("flats claim");
            verifyMessageEquals("claim.success");
            assertTrue(createdFlat.isOwner(player), "Player should be the owner of the claimed flat.");
        }

        @Test
        @DisplayName("Cannot claim more flats than the limit")
        void claimLimit() {
            for (int i = 0; i < 3; i++) {
                Flat flat = createAndClaimFlat();
                assertTrue(flat.isOwner(player), "Player should be the owner of the claimed flat " + (i + 1));
            }
            Flat fourthFlat = createValidFlat();
            placePlayerInFlat();

            executeCommand("flats claim");
            verifyMessageEquals("claim.max_claimable_flats_reached", plugin.getSettingsConfig().getMaxClaimableFlats());
            assertFalse(fourthFlat.isOwner(player), "Player should not be able to claim more than the limit of flats");
        }

        @Test
        @DisplayName("Unclaim command removes ownership")
        void unclaimCommand() {
            Flat flat = createValidFlat();
            flat.setOwner(player);
            placePlayerInFlat();

            executeCommand("flats unclaim");
            verifyMessageEquals("unclaim.success");
            assertFalse(flat.hasOwner(), "Flat should no longer have an owner after unclaiming.");
        }

        @Test
        @DisplayName("Cannot unclaim flat owned by another player")
        void unclaimCommandWithoutOwnership() {
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
        void trustCommandWithOnlineTarget() {
            createAndClaimFlat();

            executeCommand("flats trust " + target.getName());
            verifyMessageEquals("trust.success", target.getName());

            Flat flat = flatsCache.getExistingFlat(testFlatName);
            assertTrue(flat.isTrusted(target), "Target player should be trusted in the flat.");
        }

        @Test
        @DisplayName("Trust command works with offline player")
        void trustCommandWithOfflineTarget() {
            createAndClaimFlat();
            target.kick();

            executeCommand("flats trust " + target.getName());
            verifyMessageEquals("trust.success", target.getName());

            Flat flat = flatsCache.getExistingFlat(testFlatName);
            assertTrue(flat.isTrusted(target), "Offline target player should be trusted in the flat.");
        }

        @Test
        @DisplayName("Untrust command removes online player from trusted list")
        void untrustCommandWithOnlineTarget() {
            createAndClaimFlat();
            executeCommand("flats trust " + target.getName());
            verifyMessageEquals("trust.success", target.getName());

            executeCommand("flats untrust " + target.getName());
            verifyMessageEquals("untrust.success", target.getName());
            Flat flat = flatsCache.getExistingFlat(testFlatName);
            assertFalse(flat.isTrusted(target), "Target player should no longer be trusted after untrusting.");
        }

        @Test
        @DisplayName("Untrust command works with offline player")
        void untrustCommandWithOfflineTarget() {
            createAndClaimFlat();
            executeCommand("flats trust " + target.getName());
            verifyMessageEquals("trust.success", target.getName());
            target.kick();

            executeCommand("flats untrust " + target.getName());
            verifyMessageEquals("untrust.success", target.getName());
            Flat flat = flatsCache.getExistingFlat(testFlatName);
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
        void infoCommandWhenInFlat() {
            createAndClaimFlat();

            executeCommand("flats info");
            verifyMessageEquals("info.flat", testFlatName);
            verifyMessageEquals("info.owner", player.getName());
        }

        @Test
        @DisplayName("Info command shows error when player is not in a flat")
        void infoCommandWhenNotInFlat() {
            placePlayerFarFromFlats();

            executeCommand("flats info");
            verifyMessageEquals("error.not_in_flat");
        }

        @Test
        @DisplayName("List command shows all flats")
        void listCommand() {
            createValidFlat();

            executeCommandAsOp("flats list");
            verifyMessageEquals("list.title");
            verifyMessageEquals("info.flat", testFlatName);
            verifyMessageEquals("info.unoccupied");
        }

        @Test
        @DisplayName("Show command highlights nearby flats")
        void showCommand() {
            createValidFlat();
            placePlayerInFlat();
            System.out.println(selectionMaxY);
            executeCommandAsOp("flats show");
            verifyMessageEquals("show.success", 10);
            // Visual assertion isn't applicable in tests but confirm no errors occur.
        }

        @Test
        @DisplayName("Show command works when no flats are nearby")
        void showCommandNoNearbyFlats() {
            placePlayerFarFromFlats();

            executeCommandAsOp("flats show");
            verifyMessageEquals("show.success", 10);
            // Visual assertion isn't applicable in tests but confirm no errors occur.
        }
    }
}
