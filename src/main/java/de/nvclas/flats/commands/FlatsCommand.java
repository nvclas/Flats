package de.nvclas.flats.commands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.schedulers.CommandDelayScheduler;
import de.nvclas.flats.updater.UpdateDownloader;
import de.nvclas.flats.updater.UpdateStatus;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


public class FlatsCommand implements CommandExecutor, TabCompleter {

    private static final String NOT_IN_FLAT = "messages.not_in_flat";
    private final Flats flatsPlugin;
    private Player player;

    public FlatsCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
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

        switch (FlatsSubCommand.fromString(subCommand)) {
            case SELECT -> handleSelectCommand();
            case ADD -> handleAddCommand(args);
            case REMOVE -> handleRemoveCommand(args);
            case CLAIM -> handleClaimCommand();
            case UNCLAIM -> handleUnclaimCommand();
            case INFO -> handleInfoCommand();
            case LIST -> handleListCommand();
            case SHOW -> handleShowCommand();
            case UPDATE -> handleUpdateCommand();
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
        if (selection.calculateVolume() > flatsPlugin.getSettingsConfig().getMaxFlatSize()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.selection_too_large"));
            return;
        }
        if (doesSelectionIntersect(selection)) {
            return;
        }
        String flatName = args[1];
        if (!flatsPlugin.getFlatsManager().existsFlat(flatName)) {
            flatsPlugin.getFlatsManager().create(flatName, Area.fromSelection(selection, flatName));
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_created", flatName));
            return;
        }
        flatsPlugin.getFlatsManager().addArea(flatName, Area.fromSelection(selection, flatName));
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_area_added", flatName));
    }

    private boolean doesSelectionIntersect(Selection selection) {
        return flatsPlugin.getFlatsManager()
                .getAllAreas()
                .stream()
                .filter(selection::intersects)
                .findFirst()
                .map(area -> {
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_intersect"));
                    player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_intersect_details",
                            area.getFlatName(),
                            area.getLocationString()));
                    return true;
                })
                .orElse(false);
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
        if (!flatsPlugin.getFlatsManager().getAllFlatNames().contains(flatToRemove)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_not_exist"));
            return;
        }
        flatsPlugin.getFlatsManager().delete(flatToRemove);
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_deleted", flatToRemove));
    }

    private void handleClaimCommand() {
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
            return;
        }
        OfflinePlayer owner = flat.getOwner();
        if (owner == null) {
            flatsPlugin.getFlatsManager().setOwner(flat, player);
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
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
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
        flatsPlugin.getFlatsManager().setOwner(flat, null);
    }

    private void handleInfoCommand() {
        for (Area area : flatsPlugin.getFlatsManager().getAllAreas()) {
            if (area.isWithinBounds(player.getLocation())) {
                player.sendMessage(Flats.PREFIX + I18n.translate("messages.flat_info_header", area.getFlatName()));
                //noinspection DataFlowIssue
                OfflinePlayer owner = flatsPlugin.getFlatsManager().getFlat(area.getFlatName()).getOwner();
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
        if (flatsPlugin.getFlatsManager().getAllFlatNames().isEmpty()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_empty"));
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_list_title"));
        for (Flat flat : flatsPlugin.getFlatsManager().getAllFlats()) {
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

    private void handleShowCommand() {
        byte showTime = 10;

        if (CommandDelayScheduler.getDelay(player, FlatsSubCommand.SHOW.getFullCommandName()) != 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("messages.command_delay",
                    CommandDelayScheduler.getDelay(player, FlatsSubCommand.SHOW.getFullCommandName())));
            return;
        }

        if (!player.hasPermission(Permissions.ADMIN)) {
            new CommandDelayScheduler(FlatsSubCommand.SHOW.getFullCommandName(), showTime).start(player, flatsPlugin);
        }

        player.sendMessage(Flats.PREFIX + I18n.translate("messages.flats_show", showTime));

        List<Block> blocksToChange = getBlocksToChange();
        int maxUpdatesPerTick = 100;

        new BukkitRunnable() {
            private int currentIndex = 0;

            @Override
            public void run() {
                if (currentIndex >= blocksToChange.size()) {
                    cancel();
                    Bukkit.getScheduler()
                            .runTaskLater(flatsPlugin,
                                    () -> player.sendBlockChanges(blocksToChange.stream()
                                            .map(Block::getState)
                                            .toList()),
                                    20L * showTime);
                    return;
                }

                int endIndex = Math.min(blocksToChange.size(), currentIndex + maxUpdatesPerTick);
                for (int i = currentIndex; i < endIndex; i++) {
                    Block block = blocksToChange.get(i);
                    player.sendBlockChange(block.getLocation(), Material.YELLOW_STAINED_GLASS.createBlockData());
                }
                currentIndex = endIndex;
            }
        }.runTaskTimer(flatsPlugin, 0L, 1L);
    }

    private void handleUpdateCommand() {
        UpdateDownloader updateDownloader = new UpdateDownloader(flatsPlugin,
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

        flatsPlugin.getFlatsManager().getAllAreas()
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
                completions.addAll(List.of(FlatsSubCommand.SELECT.getSubCommandName(),
                        FlatsSubCommand.ADD.getSubCommandName(),
                        FlatsSubCommand.REMOVE.getSubCommandName(),
                        FlatsSubCommand.LIST.getSubCommandName(),
                        FlatsSubCommand.UPDATE.getSubCommandName()));
            }
            completions.addAll(List.of(FlatsSubCommand.CLAIM.getSubCommandName(),
                    FlatsSubCommand.UNCLAIM.getSubCommandName(),
                    FlatsSubCommand.INFO.getSubCommandName(),
                    FlatsSubCommand.SHOW.getSubCommandName()));

            completions = completions.stream()
                    .filter(completion -> completion.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase(FlatsSubCommand.REMOVE.getSubCommandName()) && sender.hasPermission(
                Permissions.ADMIN)) {
            completions = flatsPlugin.getFlatsManager().getAllFlatNames()
                    .stream()
                    .filter(flatName -> flatName.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return completions;
    }
}