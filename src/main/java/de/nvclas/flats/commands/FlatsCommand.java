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
    
    public FlatsCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!command.getName().equalsIgnoreCase("flats") || !(sender instanceof Player player)) {
            return false;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (FlatsSubCommand.fromString(subCommand)) {
            case SELECT -> handleSelectCommand(player);
            case ADD -> handleAddCommand(player, args);
            case REMOVE -> handleRemoveCommand(player, args);
            case CLAIM -> handleClaimCommand(player);
            case UNCLAIM -> handleUnclaimCommand(player);
            case TRUST -> handleTrustCommand(player, args);
            case UNTRUST -> handleUntrustCommand(player, args);
            case INFO -> handleInfoCommand(player);
            case LIST -> handleListCommand(player);
            case SHOW -> handleShowCommand(player);
            case UPDATE -> handleUpdateCommand(player);
            default -> sendHelpMessage(player);
        }
        return true;
    }

    private void handleSelectCommand(Player player) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        player.getInventory().addItem(SelectionItem.getItem());
    }

    private void handleAddCommand(Player player, String[] args) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("add.usage"));
            return;
        }

        Selection selection = Selection.getSelection(player);
        int volume = selection.calculateVolume();
        if (volume == 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.nothing_selected"));
            return;
        }
        if (volume > flatsPlugin.getSettingsConfig().getMaxFlatSize()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.selection_too_large"));
            return;
        }
        if (doesSelectionIntersect(player, selection)) {
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

    private boolean doesSelectionIntersect(Player player, Selection selection) {
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

    private void handleRemoveCommand(Player player, String[] args) {
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

    private void handleClaimCommand(Player player) {
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
            return;
        }
        if (flat.isOwner(player)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("claim.already_your_flat"));
            return;
        }
        if (flat.hasOwner()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("claim.already_owned_by", flat.getOwner().getName()));
            return;
        }
        flat.setOwner(player);
        player.sendMessage(Flats.PREFIX + I18n.translate("claim.success"));
    }

    private void handleUnclaimCommand(Player player) {
        Flat flat = getOwnedFlatAtPlayerLocation(player);
        if (flat == null) {
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("unclaim.success"));
        flat.setOwner(null);
        flat.getTrusted().clear();

    }

    private void handleTrustCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("trust.usage"));
            return;
        }
        Flat flat = getOwnedFlatAtPlayerLocation(player);
        if (flat == null) {
            return;
        }
        OfflinePlayer target = findOfflinePlayer(player, args[1]);
        if (target == null) return;
        if (flat.isTrusted(target)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("trust.already_trusted", target.getName()));
            return;
        }
        flatsPlugin.getFlatsManager().addTrusted(flat.getName(), target);
        player.sendMessage(Flats.PREFIX + I18n.translate("trust.success", target.getName()));
    }

    private void handleUntrustCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Flats.PREFIX + I18n.translate("untrust.usage"));
            return;
        }
        Flat flat = getOwnedFlatAtPlayerLocation(player);
        if (flat == null) {
            return;
        }
        OfflinePlayer target = findOfflinePlayer(player, args[1]);
        if (target == null) return;
        if (!flat.isTrusted(target)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("untrust.not_trusted", target.getName()));
            return;
        }
        flatsPlugin.getFlatsManager().removeTrusted(flat.getName(), target);
        player.sendMessage(Flats.PREFIX + I18n.translate("untrust.success", target.getName()));
    }

    @SuppressWarnings("DataFlowIssue")
    private void handleInfoCommand(Player player) {
        for (Area area : flatsPlugin.getFlatsManager().getAllAreas()) {
            if (area.isWithinBounds(player.getLocation())) {
                player.sendMessage(Flats.PREFIX + I18n.translate("info.flat", area.getFlatName()));
                OfflinePlayer owner = flatsPlugin.getFlatsManager().getFlat(area.getFlatName()).getOwner();
                if (owner == null) {
                    player.sendMessage(Flats.PREFIX + I18n.translate("info.unoccupied"));
                } else {
                    player.sendMessage(Flats.PREFIX + I18n.translate("info.owner", owner.getName()));
                }
                listAllTrustedOfFlat(player, flatsPlugin.getFlatsManager().getFlat(area.getFlatName()));
                player.sendMessage(Flats.PREFIX + I18n.translate("info.area", area.getLocationString()));
                return;
            }
        }
        player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
    }

    private void handleListCommand(Player player) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        if (flatsPlugin.getFlatsManager().getAllFlatNames().isEmpty()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("list.empty"));
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("list.title"));
        for (Flat flat : flatsPlugin.getFlatsManager().getAllFlats()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("info.flat", flat.getName()));
            if (!flat.hasOwner()) {
                player.sendMessage(Flats.PREFIX + I18n.translate("info.unoccupied"));
            } else {
                player.sendMessage(Flats.PREFIX + I18n.translate("info.owner", flat.getOwner().getName()));
            }
            listAllAreasOfFlat(player, flat);
        }
    }

    private void handleShowCommand(Player player) {
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

        List<Block> blocksToChange = getBlocksToChange(player);
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

    private void handleUpdateCommand(Player player) {
        if (Permissions.hasNoPermission(player, Permissions.ADMIN)) {
            return;
        }
        UpdateDownloader updateDownloader = new UpdateDownloader(flatsPlugin,
                "https://api.github.com/repos/nvclas/Flats/releases/latest");
        UpdateStatus status = updateDownloader.downloadLatestRelease();
        switch (status) {
            case SUCCESS -> {
                updateDownloader.unloadPluginAndDeleteJar();
                player.sendMessage(Flats.PREFIX + I18n.translate("update.success", updateDownloader.getFileName()));
            }
            case NOT_FOUND -> player.sendMessage(Flats.PREFIX + I18n.translate("update.not_found"));
            case FAILED -> player.sendMessage(Flats.PREFIX + I18n.translate("update.failed"));
        }
    }

    private @NotNull List<Block> getBlocksToChange(Player player) {
        List<Block> blocksToChange = new ArrayList<>();

        flatsPlugin.getFlatsManager()
                .getAllAreas()
                .stream()
                .filter(area -> area.isWithinDistance(player.getLocation(), 100))
                .forEach(area -> blocksToChange.addAll(area.getAllOuterBlocks()));

        return blocksToChange;
    }

    private void listAllTrustedOfFlat(Player player, Flat flat) {
        if(flat.getTrusted().isEmpty()) {
            return;
        }
        player.sendMessage(Flats.PREFIX + I18n.translate("info.trusted_header"));
        for (OfflinePlayer trustedPlayer : flat.getTrusted()) {
            String messageKey = flat.getTrusted()
                    .getLast() == trustedPlayer ? "info.trusted_last" : "info.trusted_item";
            player.sendMessage(Flats.PREFIX + I18n.translate(messageKey, trustedPlayer.getName()));
        }
    }

    private void listAllAreasOfFlat(Player player, Flat flat) {
        player.sendMessage(Flats.PREFIX + I18n.translate("list.areas_header"));
        for (Area area : flat.getAreas()) {
            String messageKey = flat.getAreas().getLast() == area ? "list.areas_last" : "list.areas_item";
            player.sendMessage(Flats.PREFIX + I18n.translate(messageKey, area.getLocationString()));
        }
    }
    
    private @Nullable OfflinePlayer findOfflinePlayer(Player player, String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
        if (target == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.player_not_found", name));
            return null;
        }
        return target;
    }

    private @Nullable Flat getOwnedFlatAtPlayerLocation(Player player) {
        Flat flat = flatsPlugin.getFlatsManager().getFlatByLocation(player.getLocation());
        if (flat == null) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_IN_FLAT));
            return null;
        }
        if (!flat.isOwner(player)) {
            player.sendMessage(Flats.PREFIX + I18n.translate(NOT_YOUR_FLAT));
            return null;
        }
        return flat;
    }

    private void sendHelpMessage(Player player) {
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
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
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
                    FlatsSubCommand.TRUST.getSubCommandName(),
                    FlatsSubCommand.UNTRUST.getSubCommandName(),
                    FlatsSubCommand.INFO.getSubCommandName(),
                    FlatsSubCommand.SHOW.getSubCommandName()));

            completions = completions.stream()
                    .filter(completion -> completion.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase(FlatsSubCommand.REMOVE.getSubCommandName()) && sender.hasPermission(
                Permissions.ADMIN)) {
            completions = flatsPlugin.getFlatsManager()
                    .getAllFlatNames()
                    .stream()
                    .filter(flatName -> flatName.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return completions;
    }
}