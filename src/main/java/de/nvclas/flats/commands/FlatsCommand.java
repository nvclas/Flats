package de.nvclas.flats.commands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.selection.Selection;
import de.nvclas.flats.updater.UpdateDownloader;
import de.nvclas.flats.updater.UpdateStatus;
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
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlatsCommand implements CommandExecutor, TabCompleter {

    private static final String NOT_IN_FLAT = "messages.not_in_flat";

    private final FlatsConfig flatsConfig;
    private final SettingsConfig settingsConfig;
    private final Flats plugin;

    private Player player;

    public FlatsCommand(@NotNull Flats plugin) {
        this.plugin = plugin;
        this.flatsConfig = plugin.getFlatsConfig();
        this.settingsConfig = plugin.getSettingsConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("flats") || !(sender instanceof Player p)) {
            return false;
        }

        player = p;

        if (args.length == 0) {
            sendHelpMessage();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "select" -> handleSelectCommand();
            case "add" -> handleAddCommand(args);
            case "remove" -> handleRemoveCommand(args);
            case "claim" -> handleClaimCommand();
            case "unclaim" -> handleUnclaimCommand();
            case "info" -> handleInfoCommand();
            case "list" -> handleListCommand();
            case "show" -> handleShowCommand();
            case "update" -> handleUpdateCommand();
            default -> sendHelpMessage();
        }
        return true;
    }

    private void handleSelectCommand() {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        player.getInventory().addItem(SelectionItem.getItem());
    }

    private void handleAddCommand(String[] args) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (Selection.getSelection(player).calculateVolume() == 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.nothing_selected"));
            return;
        }
        if (Selection.getSelection(player).calculateVolume() > settingsConfig.getMaxFlatSize()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.selection_too_large"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.use_command"));
            return;
        }
        String flatName = args[1];
        if (doesSelectionIntersect()) {
            return;
        }
        flatsConfig.addSelection(flatName, Selection.getSelection(player));
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_created", flatName));
    }

    private boolean doesSelectionIntersect() {
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

    private void handleRemoveCommand(String[] args) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
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

    private void handleClaimCommand() {
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

    private void handleUnclaimCommand() {
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

    private void handleInfoCommand() {
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

    private void handleListCommand() {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (flatsConfig.getConfigFile().getKeys(false).isEmpty()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_empty"));
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

    public void handleShowCommand() {
        byte showTime = 10;
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_show", showTime));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<Block> blocksToChange = getBlocksToChange();
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Block block : blocksToChange) {
                    player.sendBlockChange(block.getLocation(), Material.YELLOW_STAINED_GLASS.createBlockData());
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Block block : blocksToChange) {
                        player.sendBlockChange(block.getLocation(), block.getBlockData());
                    }
                }, 20L * showTime);
            });
        });
    }

    public void handleUpdateCommand() {
        UpdateDownloader updateDownloader = new UpdateDownloader(plugin);
        UpdateStatus status = updateDownloader.downloadLatestRelease();
        switch (status) {
            case SUCCESS -> {
                updateDownloader.unloadPluginAndDeleteJar();
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.update_success", updateDownloader.getFileName()));
            }
            case NOT_FOUND -> player.sendMessage(Flats.PREFIX + I18n.translate("messages.update_notfound"));
            case FAILED -> player.sendMessage(Flats.PREFIX + I18n.translate("messages.update_failed"));
        }
    }

    private @NotNull Set<Block> getBlocksToChange() {
        Set<Block> blocksToChange = new HashSet<>();
        for (String flat : flatsConfig.getConfigFile().getKeys(false)) {
            for (String selectionString : flatsConfig.getAreas(flat)) {
                Selection selection = LocationConverter.getSelectionFromString(selectionString);
                if (selection.getPos1().distance(player.getLocation()) > 100 && selection.getPos2().distance(player.getLocation()) > 100) {
                    continue;
                }
                blocksToChange.addAll(selection.getBlockList());
            }
        }
        return blocksToChange;
    }

    private void sendHelpMessage() {
        player.sendMessage(Flats.PREFIX + I18n.translate("commands.help.header"));
        if (player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(I18n.translate("commands.help.select"));
            player.sendMessage(I18n.translate("commands.help.add"));
            player.sendMessage(I18n.translate("commands.help.remove"));
            player.sendMessage(I18n.translate("commands.help.list"));
            player.sendMessage(I18n.translate("commands.help.update"));
        }
        player.sendMessage(I18n.translate("commands.help.claim"));
        player.sendMessage(I18n.translate("commands.help.unclaim"));
        player.sendMessage(I18n.translate("commands.help.info"));
        player.sendMessage(I18n.translate("commands.help.show"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("flats")) {
            return null;
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission(Permissions.ADMIN)) {
                completions.add("select");
                completions.add("add");
                completions.add("remove");
                completions.add("list");
                completions.add("update");
            }
            completions.add("claim");
            completions.add("unclaim");
            completions.add("info");
            completions.add("show");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove") && sender.hasPermission(Permissions.ADMIN)) {
            completions.addAll(flatsConfig.getConfigFile().getKeys(false));
        }
        return completions;
    }
}