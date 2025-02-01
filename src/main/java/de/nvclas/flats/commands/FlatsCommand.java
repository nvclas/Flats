package de.nvclas.flats.commands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.managers.FlatsManager;
import de.nvclas.flats.updater.UpdateDownloader;
import de.nvclas.flats.updater.UpdateStatus;
import de.nvclas.flats.utils.I18n;
import de.nvclas.flats.utils.Permissions;
import de.nvclas.flats.volumes.Area;
import de.nvclas.flats.volumes.Flat;
import de.nvclas.flats.volumes.Selection;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class FlatsCommand implements CommandExecutor, TabCompleter {

    private static final String NOT_IN_FLAT = "messages.not_in_flat";

    private final Flats plugin = Flats.getInstance();

    private Player player;

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
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.use_command"));
            return;
        }

        Selection selection = Selection.getSelection(player);
        if (selection.calculateVolume() == 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.nothing_selected"));
            return;
        }
        if (selection.calculateVolume() > Flats.getInstance().getSettingsConfig().getMaxFlatSize()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.selection_too_large"));
            return;
        }
        if (doesSelectionIntersect(selection)) {
            return;
        }
        String flatName = args[1];
        if (!FlatsManager.existisFlat(flatName)) {
            FlatsManager.create(flatName, Area.fromSelection(selection, flatName));
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_created", flatName));
            return;
        }
        FlatsManager.addArea(flatName, Area.fromSelection(selection, flatName));
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_area_added", flatName));
    }

    private boolean doesSelectionIntersect(Selection selection) {
        return FlatsManager.getAllAreas().stream().filter(selection::intersects).findFirst().map(area -> {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_intersect"));
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_intersect_details",
                    area.getFlatName(),
                    area.getLocationString()));
            return true;
        }).orElse(false);
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
        if (!FlatsManager.getAllFlatNames().contains(flatToRemove)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_not_exist"));
            return;
        }
        FlatsManager.delete(flatToRemove);
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_deleted", flatToRemove));
    }

    private void handleClaimCommand() {
        Flat flat = FlatsManager.getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
            return;
        }
        OfflinePlayer owner = flat.getOwner();
        if (owner == null) {
            FlatsManager.setOwner(flat, player);
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.claim_success"));
            return;
        }
        if (owner.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.already_your_flat"));
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.already_owned_by", owner.getName()));
    }

    private void handleUnclaimCommand() {
        Flat flat = FlatsManager.getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
            return;
        }
        OfflinePlayer owner = flat.getOwner();
        if (owner == null || !owner.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.not_your_flat"));
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.unclaim_success"));
        FlatsManager.setOwner(flat, null);
    }

    private void handleInfoCommand() {
        for (Area area : FlatsManager.getAllAreas()) {
            if (area.isWithinBounds(player.getLocation())) {
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_info_header", area.getFlatName()));
                OfflinePlayer owner = FlatsManager.getFlat(area.getFlatName()).getOwner();
                if (owner == null) {
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_info_unoccupied"));
                } else {
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_info_owner", owner.getName()));
                }
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_info_area", area.getLocationString()));
                return;
            }
        }
        player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
    }

    private void handleListCommand() {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (FlatsManager.getAllFlatNames().isEmpty()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_empty"));
            return;
        }
        for (Flat flat : FlatsManager.getAllFlats()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_header", flat.getName()));
            OfflinePlayer owner = flat.getOwner();
            if (owner == null) {
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_unoccupied"));
            } else {
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_owner", owner.getName()));
            }
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_areas_header"));
            for (Area area : flat.getAreas()) {
                if (flat.getAreas().getLast() == area) {
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_area_last",
                            area.getLocationString()));
                    break;
                }
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_area_item",
                        area.getLocationString()));
            }
        }
    }

    public void handleShowCommand() {
        byte showTime = 10;
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_show", showTime));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Block> blocksToChange = getBlocksToChange();

            AtomicInteger blockUpdateDelay = new AtomicInteger(1);
            int maxUpdatesPerTick = 50;

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (int i = 0; i < blocksToChange.size(); i += maxUpdatesPerTick) {
                    int startIndex = i;
                    int endIndex = Math.min(blocksToChange.size(), i + maxUpdatesPerTick);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int j = startIndex; j < endIndex; j++) {
                            Block block = blocksToChange.get(j);
                            player.sendBlockChange(block.getLocation(),
                                    Material.YELLOW_STAINED_GLASS.createBlockData());
                        }
                    }, blockUpdateDelay.getAndIncrement());
                }

                Bukkit.getScheduler()
                        .runTaskLater(plugin,
                                () -> player.sendBlockChanges(blocksToChange.stream().map(Block::getState).toList()),
                                20L * showTime);
            });
        });
    }

    public void handleUpdateCommand() {
        UpdateDownloader updateDownloader = new UpdateDownloader(plugin,
                "https://api.github.com/repos/nvclas/Flats/releases/latest");
        UpdateStatus status = updateDownloader.downloadLatestRelease();
        switch (status) {
            case SUCCESS -> {
                updateDownloader.unloadPluginAndDeleteJar();
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.update_success",
                        updateDownloader.getFileName()));
            }
            case NOT_FOUND -> player.sendMessage(Flats.PREFIX + I18n.translate("messages.update_notfound"));
            case FAILED -> player.sendMessage(Flats.PREFIX + I18n.translate("messages.update_failed"));
        }
    }

    private @NotNull List<Block> getBlocksToChange() {
        List<Block> blocksToChange = new ArrayList<>();

        FlatsManager.getAllAreas()
                .stream()
                .filter(area -> area.isWithinDistance(player.getLocation(), 100))
                .forEach(area -> blocksToChange.addAll(area.getAllOuterBlocks()));

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
            String partial = args[1].toLowerCase();
            completions = FlatsManager.getAllFlatNames()
                    .stream()
                    .filter(flatName -> flatName.toLowerCase().startsWith(partial))
                    .toList();

        }
        return completions;
    }
}