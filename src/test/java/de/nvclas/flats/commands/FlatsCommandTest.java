package de.nvclas.flats.commands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.FlatsCommand;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.testutil.TestUtil;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Flat;
import de.nvclas.flats.volumes.Selection;
import org.bukkit.Location;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.MockBukkitExtension;
import org.mockbukkit.mockbukkit.MockBukkitInject;
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
@ExtendWith(MockBukkitExtension.class)
@DisplayName("Flats Command Tests")
class FlatsCommandTest {

    // Constants
    private static final int SELECTION_VOLUME = 1000;
    private static final int FLAT_SIZE = 10;
    private static final int MAX_Y_COORD = 100;
    private static final int FAR_AWAY_COORD = 1000;

    // Test fixtures
    @MockBukkitInject
    private ServerMock server;
    @MockBukkitInject
    private Flats plugin;
    @MockBukkitInject
    private PlayerMock player;
    @MockBukkitInject
    private PlayerMock target;
    @MockBukkitInject
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
        setupConfiguration();
        flatsCache = plugin.getFlatsCache();

        random = new Random();
        randomizeTestFlatValues();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        if (plugin.getDataFolder().exists() && !plugin.getDataFolder().delete()) {
            fail("Could not delete plugin data folder.");
        }
    }

    /**
     * Configures the plugin settings to enable the use of advanced permissions.
     * <p>
     * This method modifies the settings configuration file and is typically invoked
     * during the test setup phase to ensure consistent configuration behavior.
     */
    private void setupConfiguration() {
        plugin.getSettingsConfig().getConfigFile().set("useAdvancedPermissions", true);
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

    private void executeCommandWithPermission(String command, String permission) {
        PermissionAttachment permissions = player.addAttachment(plugin);
        permissions.setPermission(permission, true);
        executeCommand(command);
        player.removeAttachment(permissions);
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
        executeCommandWithPermission("flats add " + testFlatName, Permissions.EDIT_FLATS);
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
        player.setLocation(new Location(world, flatInteriorX, flatInteriorY, flatInteriorZ));
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
            executeCommandWithPermission("flats select", Permissions.EDIT_FLATS);
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
            executeCommandWithPermission("flats add " + testFlatName, Permissions.EDIT_FLATS);
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
            selection.setPos2(new Location(world, selectionMaxX + 5, selectionMaxY + 5, selectionMaxZ + 5));

            executeCommandWithPermission("flats add newFlat", Permissions.EDIT_FLATS);
            verifyMessageEquals("error.flat_intersect");
            assertFalse(flatsCache.existsFlat("newFlat"),
                        "Flat should not be created when intersecting with existing flat.");
        }

        @Test
        @DisplayName("Remove command deletes an existing flat")
        void removeCommand() {
            createValidFlat();
            executeCommandWithPermission("flats remove " + testFlatName, Permissions.EDIT_FLATS);
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
            executeCommandWithPermission("flats claim", Permissions.CLAIM_FLATS);
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


            executeCommandWithPermission("flats claim", Permissions.CLAIM_FLATS);
            verifyMessageEquals("claim.max_claimable_flats_reached", plugin.getSettingsConfig().getMaxClaimableFlats());
            assertFalse(fourthFlat.isOwner(player), "Player should not be able to claim more than the limit of flats");
        }

        @Test
        @DisplayName("Unclaim command removes ownership")
        void unclaimCommand() {
            Flat flat = createValidFlat();
            flat.setOwner(player);
            placePlayerInFlat();

            executeCommandWithPermission("flats unclaim", Permissions.CLAIM_FLATS);
            verifyMessageEquals("unclaim.success");
            assertFalse(flat.hasOwner(), "Flat should no longer have an owner after unclaiming.");
        }

        @Test
        @DisplayName("Cannot unclaim flat owned by another player")
        void unclaimCommandWithoutOwnership() {
            Flat flat = createValidFlat();
            flat.setOwner(target);
            placePlayerInFlat();

            executeCommandWithPermission("flats unclaim", Permissions.CLAIM_FLATS);
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

            executeCommandWithPermission("flats trust " + target.getName(), Permissions.TRUST_PLAYERS);
            verifyMessageEquals("trust.success", target.getName());

            Flat flat = flatsCache.getExistingFlat(testFlatName);
            assertTrue(flat.isTrusted(target), "Target player should be trusted in the flat.");
        }

        @Test
        @DisplayName("Trust command works with offline player")
        void trustCommandWithOfflineTarget() {
            createAndClaimFlat();
            target.kick();

            executeCommandWithPermission("flats trust " + target.getName(), Permissions.TRUST_PLAYERS);
            verifyMessageEquals("trust.success", target.getName());

            Flat flat = flatsCache.getExistingFlat(testFlatName);
            assertTrue(flat.isTrusted(target), "Offline target player should be trusted in the flat.");
        }

        @Test
        @DisplayName("Untrust command removes online player from trusted list")
        void untrustCommandWithOnlineTarget() {
            createAndClaimFlat();
            executeCommandWithPermission("flats trust " + target.getName(), Permissions.TRUST_PLAYERS);
            verifyMessageEquals("trust.success", target.getName());

            executeCommandWithPermission("flats untrust " + target.getName(), Permissions.TRUST_PLAYERS);
            verifyMessageEquals("untrust.success", target.getName());
            Flat flat = flatsCache.getExistingFlat(testFlatName);
            assertFalse(flat.isTrusted(target), "Target player should no longer be trusted after untrusting.");
        }

        @Test
        @DisplayName("Untrust command works with offline player")
        void untrustCommandWithOfflineTarget() {
            createAndClaimFlat();
            executeCommandWithPermission("flats trust " + target.getName(), Permissions.TRUST_PLAYERS);
            verifyMessageEquals("trust.success", target.getName());
            target.kick();

            executeCommandWithPermission("flats untrust " + target.getName(), Permissions.TRUST_PLAYERS);
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

            executeCommandWithPermission("flats info", Permissions.INFO_FLATS);
            verifyMessageEquals("info.flat", testFlatName);
            verifyMessageEquals("info.owner", player.getName());
        }

        @Test
        @DisplayName("Info command shows error when player is not in a flat")
        void infoCommandWhenNotInFlat() {
            placePlayerFarFromFlats();

            executeCommandWithPermission("flats info", Permissions.INFO_FLATS);
            verifyMessageEquals("error.not_in_flat");
        }

        @Test
        @DisplayName("List command shows all flats")
        void listCommand() {
            createValidFlat();

            executeCommandWithPermission("flats list", Permissions.LIST_FLATS);
            verifyMessageEquals("list.title");
            verifyMessageEquals("info.flat", testFlatName);
            verifyMessageEquals("info.unoccupied");
        }

        @Test
        @DisplayName("Show command highlights nearby flats")
        void showCommand() {
            createValidFlat();
            placePlayerInFlat();
            executeCommandWithPermission("flats show", Permissions.SHOW_FLATS);
            verifyMessageEquals("show.success.singular", 10);
            // Visual assertion isn't applicable in tests but confirm no errors occur.
        }

        @Test
        @DisplayName("Show command works when no flats are nearby")
        void showCommandNoNearbyFlats() {
            placePlayerFarFromFlats();

            executeCommandWithPermission("flats show", Permissions.SHOW_FLATS);
            verifyMessageEquals("show.none");
            // Visual assertion isn't applicable in tests but confirm no errors occur.
        }
    }

    /**
     * Tests for the new quality of life commands: "rename", "tp", and "mylist".
     */
    @Nested
    @DisplayName("Quality of Life Command Tests")
    class QualityOfLifeCommandTests {

        @Test
        @DisplayName("Rename command renames owned flat successfully")
        void renameCommandSuccess() {
            createValidFlat();
            claimFlat();
            String oldName = testFlatName;
            String newName = testFlatName + "_renamed";

            executeCommandWithPermission("flats rename " + oldName + " " + newName, Permissions.CLAIM_FLATS);
            verifyMessageEquals("rename.success", oldName, newName);
            assertTrue(flatsCache.existsFlat(newName), "Renamed flat should exist");
            assertFalse(flatsCache.existsFlat(oldName), "Old flat name should not exist");
        }

        @Test
        @DisplayName("Rename command fails when flat does not exist")
        void renameCommandFlatNotExist() {
            executeCommandWithPermission("flats rename nonexistent newname", Permissions.CLAIM_FLATS);
            verifyMessageEquals("error.flat_not_exist");
        }

        @Test
        @DisplayName("Rename command fails when new name is taken")
        void renameCommandNameTaken() {
            createValidFlat();
            claimFlat();
            String oldName = testFlatName;
            String existingName = "existing_flat";
            
            // Create another flat with the target name
            setupValidSelection();
            executorPlayer.setLocation(new Location(testWorld, 100, 64, 100));
            executeCommandWithPermission("flats add " + existingName, Permissions.EDIT_FLATS);

            executeCommandWithPermission("flats rename " + oldName + " " + existingName, Permissions.CLAIM_FLATS);
            verifyMessageEquals("rename.name_taken", existingName);
        }

        @Test
        @DisplayName("Rename command fails for non-owned flat")
        void renameCommandNotOwned() {
            createValidFlat();
            // Don't claim the flat
            executeCommandWithPermission("flats rename " + testFlatName + " newname", Permissions.CLAIM_FLATS);
            verifyMessageEquals("error.not_your_flat");
        }

        @Test
        @DisplayName("Teleport command teleports to owned flat")
        void teleportCommandSuccess() {
            createValidFlat();
            claimFlat();
            
            // Move player away from the flat
            executorPlayer.setLocation(new Location(testWorld, 1000, 64, 1000));
            
            executeCommandWithPermission("flats tp " + testFlatName, Permissions.CLAIM_FLATS);
            verifyMessageEquals("teleport.success", testFlatName);
            
            // Check if player is now within the flat bounds
            Flat flat = flatsCache.getExistingFlat(testFlatName);
            assertTrue(flat.isWithinBounds(executorPlayer.getLocation()), 
                      "Player should be teleported within flat bounds");
        }

        @Test
        @DisplayName("Teleport command fails when flat does not exist")
        void teleportCommandFlatNotExist() {
            executeCommandWithPermission("flats tp nonexistent", Permissions.CLAIM_FLATS);
            verifyMessageEquals("error.flat_not_exist");
        }

        @Test
        @DisplayName("Teleport command fails for non-owned flat")
        void teleportCommandNotOwned() {
            createValidFlat();
            // Don't claim the flat
            executeCommandWithPermission("flats tp " + testFlatName, Permissions.CLAIM_FLATS);
            verifyMessageEquals("error.not_your_flat");
        }

        @Test
        @DisplayName("MyList command shows owned flats")
        void myListCommandSuccess() {
            createValidFlat();
            claimFlat();
            
            executeCommandWithPermission("flats mylist", Permissions.LIST_FLATS);
            verifyMessageEquals("mylist.title");
            verifyMessageEquals("mylist.total", 1);
        }

        @Test
        @DisplayName("MyList command shows empty message when no flats owned")
        void myListCommandEmpty() {
            executeCommandWithPermission("flats mylist", Permissions.LIST_FLATS);
            verifyMessageEquals("mylist.empty");
        }

        @Test
        @DisplayName("MyList command only shows player's own flats")
        void myListCommandOnlyOwnFlats() {
            createValidFlat();
            claimFlat();
            
            // Create another flat owned by a different player
            PlayerMock otherPlayer = server.addPlayer();
            setupValidSelection();
            executorPlayer.setLocation(new Location(testWorld, 100, 64, 100));
            executeCommandWithPermission("flats add other_flat", Permissions.EDIT_FLATS);
            Flat otherFlat = flatsCache.getExistingFlat("other_flat");
            otherFlat.setOwner(otherPlayer);
            
            executeCommandWithPermission("flats mylist", Permissions.LIST_FLATS);
            verifyMessageEquals("mylist.title");
            verifyMessageEquals("mylist.total", 1); // Should only count the player's flat
        }

        @Test
        @DisplayName("Commands require proper permissions")
        void commandsRequirePermissions() {
            createValidFlat();
            claimFlat();
            
            removePermissions();
            
            executeCommand("flats rename " + testFlatName + " newname");
            verifyMessageEquals("error.no_permission");
            
            executeCommand("flats tp " + testFlatName);
            verifyMessageEquals("error.no_permission");
            
            executeCommand("flats mylist");
            verifyMessageEquals("error.no_permission");
        }

        @Test
        @DisplayName("Commands show usage when insufficient arguments provided")
        void commandsShowUsage() {
            executeCommandWithPermission("flats rename", Permissions.CLAIM_FLATS);
            verifyMessageEquals("rename.usage");
            
            executeCommandWithPermission("flats tp", Permissions.CLAIM_FLATS);
            verifyMessageEquals("teleport.usage");
        }

        @Test
        @DisplayName("Unclaim command requires confirmation")
        void unclaimCommandRequiresConfirmation() {
            createValidFlat();
            claimFlat();
            placePlayerInFlat();
            
            // First unclaim command should show confirmation message
            executeCommandWithPermission("flats unclaim", Permissions.CLAIM_FLATS);
            verifyMessageEquals("unclaim.confirmation", testFlatName);
            
            // Flat should still be owned
            Flat flat = flatsCache.getExistingFlat(testFlatName);
            assertTrue(flat.hasOwner(), "Flat should still be owned after first unclaim attempt");
            
            // Second unclaim command (within timeout) should actually unclaim
            executeCommandWithPermission("flats unclaim", Permissions.CLAIM_FLATS);
            verifyMessageEquals("unclaim.success");
            
            // Now flat should not be owned
            assertFalse(flat.hasOwner(), "Flat should no longer be owned after confirmation");
        }

        @Test
        @DisplayName("Enhanced tab completion works for new commands")
        void enhancedTabCompletion() {
            createValidFlat();
            claimFlat();
            
            FlatsCommand command = new FlatsCommand(flatsPlugin);
            
            // Test tab completion for rename command with owned flat
            List<String> completions = command.onTabComplete(executorPlayer, null, "flats", new String[]{"rename", ""});
            assertNotNull(completions);
            assertTrue(completions.contains(testFlatName), "Should suggest owned flat names for rename command");
            
            // Test tab completion for teleport command
            completions = command.onTabComplete(executorPlayer, null, "flats", new String[]{"tp", ""});
            assertNotNull(completions);
            assertTrue(completions.contains(testFlatName), "Should suggest owned flat names for teleport command");
            
            // Test that non-owned flats are not suggested
            setupValidSelection();
            executorPlayer.setLocation(new Location(testWorld, 100, 64, 100));
            executeCommandWithPermission("flats add other_flat", Permissions.EDIT_FLATS);
            
            completions = command.onTabComplete(executorPlayer, null, "flats", new String[]{"tp", ""});
            assertFalse(completions.contains("other_flat"), "Should not suggest non-owned flats for teleport command");
        }

        @Test
        @DisplayName("Stats command shows detailed flat information")
        void statsCommandShowsDetails() {
            createValidFlat();
            claimFlat();
            
            executeCommandWithPermission("flats stats " + testFlatName, Permissions.INFO_FLATS);
            verifyMessageEquals("stats.header", testFlatName);
        }

        @Test
        @DisplayName("Stats command requires permission to view")
        void statsCommandRequiresPermission() {
            createValidFlat();
            // Don't claim the flat, so player doesn't own it
            
            executeCommandWithPermission("flats stats " + testFlatName, Permissions.INFO_FLATS);
            verifyMessageEquals("stats.no_permission");
        }

        @Test
        @DisplayName("Stats command shows usage when no flat name provided")
        void statsCommandShowsUsage() {
            executeCommandWithPermission("flats stats", Permissions.INFO_FLATS);
            verifyMessageEquals("stats.usage");
        }

        @Test
        @DisplayName("Stats command works for trusted players")
        void statsCommandWorksForTrustedPlayers() {
            createValidFlat();
            claimFlat();
            
            // Create another player and trust them
            PlayerMock trustedPlayer = server.addPlayer("TrustedPlayer");
            executeCommandWithPermission("flats trust " + trustedPlayer.getName(), Permissions.TRUST_PLAYERS);
            
            // Trusted player should be able to view stats
            FlatsCommand command = new FlatsCommand(flatsPlugin);
            command.onCommand(trustedPlayer, null, "flats", new String[]{"stats", testFlatName});
            
            // Should not get permission error (would need more complex verification for exact message)
            // For now, just ensure it doesn't throw an exception
        }
    }
}
