package de.nvclas.flats.core.commands;

import de.nvclas.flats.core.BaseFlats;
import de.nvclas.flats.core.cache.FlatsCache;
import de.nvclas.flats.core.commands.subcommands.AddSubCommand;
import de.nvclas.flats.core.commands.subcommands.ClaimSubCommand;
import de.nvclas.flats.core.commands.subcommands.InfoSubCommand;
import de.nvclas.flats.core.commands.subcommands.ListSubCommand;
import de.nvclas.flats.core.commands.subcommands.RemoveSubCommand;
import de.nvclas.flats.core.commands.subcommands.SelectSubCommand;
import de.nvclas.flats.core.commands.subcommands.ShowSubCommand;
import de.nvclas.flats.core.commands.subcommands.TrustSubCommand;
import de.nvclas.flats.core.commands.subcommands.UnclaimSubCommand;
import de.nvclas.flats.core.commands.subcommands.UntrustSubCommand;
import de.nvclas.flats.core.commands.subcommands.UpdateSubCommand;
import de.nvclas.flats.core.config.ConfigAdapter;
import de.nvclas.flats.core.util.I18n;
import de.nvclas.flats.core.util.Permission;
import de.nvclas.flats.core.volumes.Flat;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final BaseFlats plugin;
    private final ConfigAdapter configAdapter;
    private final FlatsCache flatsCache;

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public MainCommand(BaseFlats plugin) {
        this.plugin = plugin;
        this.configAdapter = plugin.getConfigAdapter();
        this.flatsCache = plugin.getFlatsCache();
        registerSubCommands();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String @NotNull [] args) {
        if (!plugin.getMainCommandName().equalsIgnoreCase(command.getName()) || !(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + I18n.translate("error.only_players"));
            return false;
        }

        if (Permission.hasZeroPermissions(plugin, player, configAdapter)) {
            Permission.showNoPermissionMessage(plugin, player);
            return true;
        }

        if (args.length == 0 || !subCommands.containsKey(args[0].toLowerCase(Locale.ROOT))) {
            sendHelpMessages(player);
            return true;
        }

        subCommands.get(args[0].toLowerCase(Locale.ROOT)).execute(player, args);
        return true;
    }

    private void sendHelpMessages(Player player) {
        player.sendMessage(plugin.getPrefix() + I18n.translate("help.header", plugin.getMainCommandName()));
        if (Permission.canEditFlats(plugin, player, configAdapter)) {
            sendEditHelpMessages(player);
        }
        if (Permission.canListFlats(plugin, player, configAdapter)) {
            sendListHelpMessages(player);
        }
        if (Permission.canInfoFlats(plugin, player, configAdapter)) {
            sendInfoHelpMessages(player);
        }
        if (Permission.canClaimFlats(plugin, player, configAdapter)) {
            sendClaimHelpMessages(player);
        }
        if (Permission.canTrustPlayers(plugin, player, configAdapter)) {
            sendTrustHelpMessages(player);
        }
        if (Permission.canShowFlats(plugin, player, configAdapter)) {
            sendShowHelpMessages(player);
        }
        if (Permission.hasAdminPermission(plugin, player)) {
            sendAdminHelpMessages(player);
        }
    }

    private void sendEditHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.select", plugin.getMainCommandName()));
        player.sendMessage(I18n.translate("help.add", plugin.getMainCommandName()));
        player.sendMessage(I18n.translate("help.remove", plugin.getMainCommandName()));
    }

    private void sendListHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.list", plugin.getMainCommandName()));
    }

    private void sendInfoHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.info", plugin.getMainCommandName()));
    }

    private void sendClaimHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.claim", plugin.getMainCommandName()));
        player.sendMessage(I18n.translate("help.unclaim", plugin.getMainCommandName()));
    }

    private void sendTrustHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.trust", plugin.getMainCommandName()));
        player.sendMessage(I18n.translate("help.untrust", plugin.getMainCommandName()));
    }

    private void sendShowHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.show", plugin.getMainCommandName()));
    }

    private void sendAdminHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.update", plugin.getMainCommandName(), plugin.getPluginName()));
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String @NotNull [] args) {
        if (!plugin.getMainCommandName().equalsIgnoreCase(command.getName()) || !(sender instanceof Player player)) {
            return null;
        }

        if (args.length == 0) {
            return List.of();
        }

        return switch (args.length) {
            case 1 -> getSubCommandCompletions(player, args[0]);
            case 2 -> getSecondArgumentCompletions(player, args[0], args[1]);
            default -> List.of();
        };
    }

    private List<String> getSubCommandCompletions(Player player, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        return subCommands.keySet()
                .stream()
                .filter(cmd -> cmd.startsWith(lowerInput) && hasPermissionForCommand(player, cmd))
                .toList();
    }

    private List<String> getSecondArgumentCompletions(Player player, String subCommand, String input) {
        if (MainSubCommand.REMOVE.getSubCommandName().equalsIgnoreCase(subCommand) && Permission.canEditFlats(plugin,
                player,
                configAdapter)) {
            return getFlatNameCompletions(input);
        }

        if (MainSubCommand.INFO.getSubCommandName().equalsIgnoreCase(subCommand) && Permission.canInfoFlats(plugin,
                player,
                configAdapter)) {
            return getFlatNameCompletions(input);
        }

        if (MainSubCommand.TRUST.getSubCommandName().equalsIgnoreCase(subCommand) && Permission.canTrustPlayers(plugin,
                player, configAdapter)) {
            return getOnlinePlayerCompletions(input);
        }

        if (MainSubCommand.UNTRUST.getSubCommandName().equalsIgnoreCase(subCommand) && Permission.canTrustPlayers(
                plugin,
                player, configAdapter)) {
            return getTrustedPlayerCompletions(player, input);
        }

        return List.of();
    }

    private List<String> getFlatNameCompletions(String input) {
        return flatsCache.getFilteredFlatNames(input, 10);
    }

    private List<String> getOnlinePlayerCompletions(String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        return plugin.getServer()
                .getOnlinePlayers()
                .stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerInput))
                .toList();
    }

    private List<String> getTrustedPlayerCompletions(Player player, String input) {
        Flat flat = flatsCache.getFlatAtLocation(player.getLocation());
        if (flat == null) {
            return List.of();
        }

        String lowerInput = input.toLowerCase(Locale.ROOT);
        return flat.getTrusted()
                .stream()
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerInput))
                .toList();
    }

    private boolean hasPermissionForCommand(Player player, String command) {
        return switch (command.toLowerCase(Locale.ROOT)) {
            case "select", "add", "remove" -> Permission.canEditFlats(plugin, player, configAdapter);
            case "list" -> Permission.canListFlats(plugin, player, configAdapter);
            case "info" -> Permission.canInfoFlats(plugin, player, configAdapter);
            case "claim", "unclaim" -> Permission.canClaimFlats(plugin, player, configAdapter);
            case "trust", "untrust" -> Permission.canTrustPlayers(plugin, player, configAdapter);
            case "show" -> Permission.canShowFlats(plugin, player, configAdapter);
            case "update" -> Permission.hasAdminPermission(plugin, player);
            default -> false;
        };
    }

    private void registerSubCommands() {
        subCommands.put(MainSubCommand.SELECT.getSubCommandName(), new SelectSubCommand(plugin));
        subCommands.put(MainSubCommand.UPDATE.getSubCommandName(), new UpdateSubCommand(plugin));
        subCommands.put(MainSubCommand.LIST.getSubCommandName(), new ListSubCommand(plugin));
        subCommands.put(MainSubCommand.INFO.getSubCommandName(), new InfoSubCommand(plugin));
        subCommands.put(MainSubCommand.SHOW.getSubCommandName(), new ShowSubCommand(plugin));
        subCommands.put(MainSubCommand.ADD.getSubCommandName(), new AddSubCommand(plugin));
        subCommands.put(MainSubCommand.REMOVE.getSubCommandName(), new RemoveSubCommand(plugin));
        subCommands.put(MainSubCommand.CLAIM.getSubCommandName(), new ClaimSubCommand(plugin));
        subCommands.put(MainSubCommand.UNCLAIM.getSubCommandName(), new UnclaimSubCommand(plugin));
        subCommands.put(MainSubCommand.TRUST.getSubCommandName(), new TrustSubCommand(plugin));
        subCommands.put(MainSubCommand.UNTRUST.getSubCommandName(), new UntrustSubCommand(plugin));
    }
}
