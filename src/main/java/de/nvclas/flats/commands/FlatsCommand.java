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

    private static final String NOT_IN_FLAT = "error.not_in_flat";
    private static final String NOT_YOUR_FLAT = "error.not_your_flat";
    
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
            case TRUST -> handleTrustCommand(args);
            case UNTRUST -> handleUntrustCommand(args);
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
            player.sendMessage(Flats.PREFIX + I18n.translate("add.usage"));
            return;
        }

        Selection selection = Selection.getSelection(player);
        if (selection.calculateVolume() == 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.nothing_selected"));
            return;
        }
        if (selection.calculateVolume() > flatsPlugin.getSettingsConfig().getMaxFlatSize()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.selection_too_large"));
            return;
        }
        if (doesSelectionIntersect(selection)) {
            return;
        }
        String flatName = args[1];
        if (!flatsPlugin.getFlatsManager().existsFlat(flatName)) {
            flatsPlugin.getFlatsManager().create(flatName, Area.fromSelection(selection, flatName));
            player.sendMessage(Flats.PREFIX + I18n.translate("add.success", flatName));
            return;
        }
        flatsPlugin.getFlatsManager().addArea(flatName, Area.fromSelection(selection, flatName));
        player.sendMessage(Flats.PREFIX + I18n.translate("add.area_added", flatName));
    }

    private boolean doesSelectionIntersect(Selection selection) {
        return flatsPlugin.getFlatsManager()
                .getAllAreas()
                .stream()
                .filter(selection::intersects)
                .findFirst()
                .map(area -> {
                    player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_intersect"));
                    player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_intersect.details",
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
            player.sendMessage(Flats.PREFIX + I18n.translate("remove.usage"));
            return;
        }
        String flatToRemove = args[1];
        if (!flatsPlugin.getFlatsManager().getAllFlatNames().contains(flatToRemove)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_not_exist"));
            return;
        }
        flatsPlugin.getFlatsManager().delete(flatToRemove);
        player.sendMessage(Flats.PREFIX + I18n.translate("remove.success", flatToRemove));
    }

    private void handleClaimCommand() {
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
            return;
        }
        if (flat.isOwner(player)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("claim.already_your_flat"));
            return;
        }
        if (!flat.hasOwner()) {
            flatsPlugin.getFlatsManager().setOwner(flat, player);
            player.sendMessage(Flats.PREFIX + I18n.translate("claim.success"));
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("claim.already_owned_by", flat.getOwner().getName()));
    }

    private void handleUnclaimCommand() {
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
            return;
        }
        if (!flat.isOwner(player)) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_YOUR_FLAT));
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("unclaim.success"));
        flatsPlugin.getFlatsManager().setOwner(flat, null);
    }

    private void handleTrustCommand(String[] args) {
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("trust.usage"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
            return;
        }
        if (!flat.isOwner(player)) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_YOUR_FLAT));
            return;
        }
        if(flat.isTrusted(target)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("trust.already_trusted", target.getName()));
            return;
        }
        // TODO: Hier muss noch getrustet werden (Manager)
    }
    
    private void handleUntrustCommand(String[] args) {
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("untrust.usage"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
            return;
        }
        if (!flat.isOwner(player)) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_YOUR_FLAT));
            return;
        }
        if(!flat.isTrusted(target)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("untrust.not_trusted", target.getName()));
            return;
        }
        // TODO: Hier muss noch enttrustet werden (Manager)
    }
    
    private void handleInfoCommand() {
        for (Area area : flatsPlugin.getFlatsManager().getAllAreas()) {
            if (area.isWithinBounds(player.getLocation())) {
                player.sendMessage(Flats.PREFIX + I18n.translate("info.header", area.getFlatName()));
                //noinspection DataFlowIssue
                OfflinePlayer owner = flatsPlugin.getFlatsManager().getFlat(area.getFlatName()).getOwner();
                if (owner == null) {
                    player.sendMessage(Flats.PREFIX + I18n.translate("info.unoccupied"));
                } else {
                    player.sendMessage(Flats.PREFIX + I18n.translate("info.owner", owner.getName()));
                }
                player.sendMessage(Flats.PREFIX + I18n.translate("info.area", area.getLocationString()));
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
            player.sendMessage(Flats.PREFIX + I18n.translate("list.empty"));
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("list.title"));
        for (Flat flat : flatsPlugin.getFlatsManager().getAllFlats()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("list.header", flat.getName()));
            if (flat.hasOwner()) {
                player.sendMessage(Flats.PREFIX + I18n.translate("list.unoccupied"));
            } else {
                player.sendMessage(Flats.PREFIX + I18n.translate("list.owner", flat.getOwner().getName()));
            }
            player.sendMessage(Flats.PREFIX + I18n.translate("list.areas_header"));
            for (Area area : flat.getAreas()) {
                if (flat.getAreas().getLast() == area) {
                    player.sendMessage(Flats.PREFIX + I18n.translate("list.area_last",
                            area.getLocationString()));
                    break;
                }
                player.sendMessage(Flats.PREFIX + I18n.translate("list.area_item",
                        area.getLocationString()));
            }
        }
    }

    private void handleShowCommand() {
        byte showTime = 10;

        if (CommandDelayScheduler.getDelay(player, FlatsSubCommand.SHOW.getFullCommandName()) != 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.command_delay",
                    CommandDelayScheduler.getDelay(player, FlatsSubCommand.SHOW.getFullCommandName())));
            return;
        }

        if (!player.hasPermission(Permissions.ADMIN)) {
            new CommandDelayScheduler(FlatsSubCommand.SHOW.getFullCommandName(), showTime).start(player, flatsPlugin);
        }

        player.sendMessage(Flats.PREFIX + I18n.translate("show.success", showTime));

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
                player.sendMessage(Flats.PREFIX + I18n.translate("update.success",
                        updateDownloader.getFileName()));
            }
            case NOT_FOUND -> player.sendMessage(Flats.PREFIX + I18n.translate("update.not_found"));
            case FAILED -> player.sendMessage(Flats.PREFIX + I18n.translate("update.failed"));
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
        player.sendMessage(Flats.PREFIX + I18n.translate("help.header"));
        if (player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(I18n.translate("help.select"));
            player.sendMessage(I18n.translate("help.add"));
            player.sendMessage(I18n.translate("help.remove"));
            player.sendMessage(I18n.translate("help.list"));
            player.sendMessage(I18n.translate("help.update"));
        }
        player.sendMessage(I18n.translate("help.claim"));
        player.sendMessage(I18n.translate("help.unclaim"));
        player.sendMessage(I18n.translate("help.trust"));
        player.sendMessage(I18n.translate("help.untrust"));
        player.sendMessage(I18n.translate("help.info"));
        player.sendMessage(I18n.translate("help.show"));
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