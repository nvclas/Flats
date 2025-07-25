package de.nvclas.flats.commands.flats;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.subcommands.AddSubCommand;
import de.nvclas.flats.commands.flats.subcommands.ClaimSubCommand;
import de.nvclas.flats.commands.flats.subcommands.InfoSubCommand;
import de.nvclas.flats.commands.flats.subcommands.ListSubCommand;
import de.nvclas.flats.commands.flats.subcommands.RemoveSubCommand;
import de.nvclas.flats.commands.flats.subcommands.SelectSubCommand;
import de.nvclas.flats.commands.flats.subcommands.ShowSubCommand;
import de.nvclas.flats.commands.flats.subcommands.TrustSubCommand;
import de.nvclas.flats.commands.flats.subcommands.UnclaimSubCommand;
import de.nvclas.flats.commands.flats.subcommands.UntrustSubCommand;
import de.nvclas.flats.commands.flats.subcommands.UpdateSubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FlatsCommand implements CommandExecutor, TabCompleter {

    private final Flats flatsPlugin;
    private final SettingsConfig settingsConfig;

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public FlatsCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        registerSubCommands();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!"flats".equalsIgnoreCase(command.getName()) || !(sender instanceof Player player)) {
            sender.sendMessage(Flats.PREFIX + I18n.translate("error.only_players"));
            return false;
        }

        if (args.length == 0 || !subCommands.containsKey(args[0].toLowerCase())) {
            sendHelpMessages(player);
            return true;
        }

        subCommands.get(args[0].toLowerCase()).execute(player, args);
        return true;
    }

    private void sendHelpMessages(Player player) {
        player.sendMessage(Flats.PREFIX + I18n.translate("help.header"));
        if (Permissions.canEditFlats(player, settingsConfig)) {
            sendEditHelpMessages(player);
        }
        if (Permissions.canListFlats(player, settingsConfig)) {
            sendListHelpMessages(player);
        }
        if (Permissions.canInfoFlats(player, settingsConfig)) {
            sendInfoHelpMessages(player);
        }
        if (Permissions.canClaimFlats(player, settingsConfig)) {
            sendClaimHelpMessages(player);
        }
        if (Permissions.canTrustPlayers(player, settingsConfig)) {
            sendTrustHelpMessages(player);
        }
        if (Permissions.canShowFlats(player, settingsConfig)) {
            sendShowHelpMessages(player);
        }
        if (Permissions.hasAdminPermission(player)) {
            sendAdminHelpMessages(player);
        }
    }

    private void sendEditHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.select"));
        player.sendMessage(I18n.translate("help.add"));
        player.sendMessage(I18n.translate("help.remove"));
    }

    private void sendListHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.list"));
    }

    private void sendInfoHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.info"));
    }

    private void sendClaimHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.claim"));
        player.sendMessage(I18n.translate("help.unclaim"));
    }

    private void sendTrustHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.trust"));
        player.sendMessage(I18n.translate("help.untrust"));
    }

    private void sendShowHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.show"));
    }

    private void sendAdminHelpMessages(Player player) {
        player.sendMessage(I18n.translate("help.update"));
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!"flats".equalsIgnoreCase(command.getName()) || !(sender instanceof Player player)) {
            return null;
        }

        if (args.length == 0) return List.of();

        List<String> completions = new ArrayList<>();
        String input = args[0].toLowerCase();

        if (args.length == 1) {
            completions = subCommands.keySet().stream()
                    .filter(cmd -> cmd.startsWith(input) && (player.hasPermission(Permissions.ADMIN) || !isAdminCommand(
                            cmd)))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase(FlatsSubCommand.REMOVE.getSubCommandName()) && player.hasPermission(
                Permissions.ADMIN)) {
            completions = flatsPlugin.getFlatsCache()
                    .getAllFlatNames()
                    .stream()
                    .filter(flatName -> flatName.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return completions;
    }

    private boolean isAdminCommand(String command) {
        return List.of(FlatsSubCommand.SELECT.getSubCommandName(),
                FlatsSubCommand.ADD.getSubCommandName(),
                FlatsSubCommand.REMOVE.getSubCommandName(),
                FlatsSubCommand.LIST.getSubCommandName(),
                FlatsSubCommand.UPDATE.getSubCommandName()).contains(command);
    }

    private void registerSubCommands() {
        subCommands.put(FlatsSubCommand.SELECT.getSubCommandName(), new SelectSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.UPDATE.getSubCommandName(), new UpdateSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.LIST.getSubCommandName(), new ListSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.INFO.getSubCommandName(), new InfoSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.SHOW.getSubCommandName(), new ShowSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.ADD.getSubCommandName(), new AddSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.REMOVE.getSubCommandName(), new RemoveSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.CLAIM.getSubCommandName(), new ClaimSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.UNCLAIM.getSubCommandName(), new UnclaimSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.TRUST.getSubCommandName(), new TrustSubCommand(flatsPlugin));
        subCommands.put(FlatsSubCommand.UNTRUST.getSubCommandName(), new UntrustSubCommand(flatsPlugin));
    }

}
