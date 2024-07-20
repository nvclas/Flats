package de.nvclas.flats.commands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.selection.Selection;
import de.nvclas.flats.utils.I18n;
import de.nvclas.flats.utils.LocationConverter;
import de.nvclas.flats.utils.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlatsCommand implements CommandExecutor {

    private static final String NOT_IN_FLAT = "messages.not_in_flat";
    
    private final FlatsConfig flatsConfig;
    private final Flats plugin;

    public FlatsCommand(Flats plugin) {
        this.plugin = plugin;
        this.flatsConfig = plugin.getFlatsConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("flat") || !(sender instanceof Player player)) {
            return false;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "select":
                handleSelectCommand(player);
                break;
            case "add":
                handleAddCommand(player, args);
                break;
            case "remove":
                handleRemoveCommand(player, args);
                break;
            case "claim":
                handleClaimCommand(player);
                break;
            case "unclaim":
                handleUnclaimCommand(player);
                break;
            case "info":
                handleInfoCommand(player);
                break;
            case "list":
                handleListCommand(player);
                break;
            case "show":
                handleShowCommand(player);
                break;
            default:
                sendHelpMessage(player);
                break;
        }
        return true;
    }

    private void handleSelectCommand(Player player) {
        if (Flats.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        player.getInventory().addItem(SelectionItem.getItem());
    }

    private void handleAddCommand(Player player, String[] args) {
        if (Flats.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (Selection.getSelection(player).calculateVolume() == 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.nothing_selected"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.use_command"));
            return;
        }
        String flatName = args[1];
        if (doesSelectionIntersect(player)) {
            return;
        }
        flatsConfig.addSelection(flatName, Selection.getSelection(player));
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_created", flatName));
    }

    private boolean doesSelectionIntersect(Player player) {
        for (String flat : flatsConfig.getConfigFile().getKeys(false)) {
            for (String selectionString : flatsConfig.getAreas(flat)) {
                Selection selection = LocationConverter.getSelectionFromString(selectionString);
                if (selection.intersects(Selection.getSelection(player))) {
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_intersect"));
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_intersect_details", flat, selectionString));
                    return true;
                }
            }
        }
        return false;
    }

    private void handleRemoveCommand(Player player, String[] args) {
        if (Flats.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.use_command"));
            return;
        }
        String flatToRemove = args[1];
        if (!flatsConfig.getConfigFile().isSet(flatToRemove)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_not_exist"));
            return;
        }
        flatsConfig.removeFlat(flatToRemove);
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_deleted", flatToRemove));
    }

    private void handleClaimCommand(Player player) {
        for (String flat : flatsConfig.getConfigFile().getKeys(false)) {
            for (String selectionString : flatsConfig.getAreas(flat)) {
                Selection selection = LocationConverter.getSelectionFromString(selectionString);
                if (selection.intersects(player.getLocation())) {
                    OfflinePlayer owner = flatsConfig.getOwner(flat);
                    if (owner == null) {
                        flatsConfig.setOwner(flat, player);
                        player.sendMessage(Flats.PREFIX + I18n.translate("messages.claim_success"));
                        return;
                    }
                    if (owner.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(Flats.PREFIX + I18n.translate("messages.already_your_flat"));
                        return;
                    }
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.already_owned_by", owner.getName()));
                    return;
                }
            }
        }
        player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
    }

    private void handleUnclaimCommand(Player player) {
        for (String flat : flatsConfig.getConfigFile().getKeys(false)) {
            for (String selectionString : flatsConfig.getAreas(flat)) {
                Selection selection = LocationConverter.getSelectionFromString(selectionString);
                if (selection.intersects(player.getLocation())) {
                    OfflinePlayer owner = flatsConfig.getOwner(flat);
                    if (owner == null || !owner.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(Flats.PREFIX + I18n.translate("messages.not_your_flat"));
                        return;
                    }
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.unclaim_success"));
                    flatsConfig.setOwner(flat, null);
                    return;
                }
            }
        }
        player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
    }

    private void handleInfoCommand(Player player) {
        for (String flat : flatsConfig.getConfigFile().getKeys(false)) {
            for (String selectionString : flatsConfig.getAreas(flat)) {
                Selection selection = LocationConverter.getSelectionFromString(selectionString);
                if (selection.intersects(player.getLocation())) {
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_info_header", flat));
                    OfflinePlayer owner = flatsConfig.getOwner(flat);
                    if (owner == null) {
                        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_info_unoccupied"));
                    } else {
                        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_info_owner", owner.getName()));
                    }
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_info_area", selectionString));
                    return;
                }
            }
        }
        player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
    }

    private void handleListCommand(Player player) {
        if (Flats.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        for (String flat : flatsConfig.getConfigFile().getKeys(false)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_header", flat));
            OfflinePlayer owner = flatsConfig.getOwner(flat);
            if (owner == null) {
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_unoccupied"));
            } else {
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_owner", owner.getName()));
            }
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_areas_header"));
            for (String selectionString : flatsConfig.getAreas(flat)) {
                if (flatsConfig.getAreas(flat).indexOf(selectionString) == flatsConfig.getAreas(flat).size() - 1) {
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_area", selectionString));
                    break;
                }
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_area_item", selectionString));
            }
        }
    }

    private void handleShowCommand(Player player) {
        for (String flat : flatsConfig.getConfigFile().getKeys(false)) {
            for (String selectionString : flatsConfig.getAreas(flat)) {
                Selection selection = LocationConverter.getSelectionFromString(selectionString);
                if (selection.getPos1().distance(player.getLocation()) > 100 && selection.getPos2().distance(player.getLocation()) > 100) {
                    continue;
                }
                for (Block block : selection.getBlockList()) {
                    player.sendBlockChange(block.getLocation(), Material.YELLOW_STAINED_GLASS.createBlockData());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendBlockChange(block.getLocation(), block.getLocation().getBlock().getBlockData()), (long) 10 * 20);
                }
            }
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(Flats.PREFIX + I18n.translate("commands.help.header"));
        if (player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(I18n.translate("commands.help.select"));
            player.sendMessage(I18n.translate("commands.help.add"));
            player.sendMessage(I18n.translate("commands.help.remove"));
            player.sendMessage(I18n.translate("commands.help.list"));
        }
        player.sendMessage(I18n.translate("commands.help.claim"));
        player.sendMessage(I18n.translate("commands.help.unclaim"));
        player.sendMessage(I18n.translate("commands.help.info"));
        player.sendMessage(I18n.translate("commands.help.show"));
    }
}