package de.nvclas.flats.commands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.selection.Selection;
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

    private final FlatsConfig flatsConfig = Flats.getInstance().getFlatsConfig();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("flat")) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "select" -> {
                if (!Flats.checkPermission(player, Permissions.ADMIN)) {
                    return true;
                }
                player.getInventory().addItem(SelectionItem.getItem());
            }

            case "add" -> {
                if (!Flats.checkPermission(player, Permissions.ADMIN)) {
                    return true;
                }
                if (Selection.getSelection(player).calculateVolume() == 0) {
                    player.sendMessage(Flats.PREFIX + "§cBruder, du hast nichts markiert!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Flats.PREFIX + "§cNutze §6/flats add <wohnungsname>");
                    return true;
                }
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.intersects(Selection.getSelection(player))) {
                            player.sendMessage(Flats.PREFIX + "§cDeine Auswahl schneidet eine andere Wohnung");
                            player.sendMessage(Flats.PREFIX + "§cWohnung: §6" + flat + " §cbei §6" + selectionString);
                            return true;
                        }
                    }
                }
                String flatName = args[1];
                flatsConfig.addSelection(flatName, Selection.getSelection(player));
                player.sendMessage(Flats.PREFIX + "§aWohnung §e" + flatName + " §awurde erstellt");
            }

            case "remove" -> {
                if (!Flats.checkPermission(player, Permissions.ADMIN)) {
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Flats.PREFIX + "§cNutze §6/flats remove <wohnungsnahme>");
                    return true;
                }
                String flatToRemove = args[1];
                if (!flatsConfig.getConfig().isSet(flatToRemove)) {
                    player.sendMessage(Flats.PREFIX + "§cBruder, diese Wohnung gibt es nicht");
                    return true;
                }
                flatsConfig.removeFlat(flatToRemove);
                player.sendMessage(Flats.PREFIX + "§aWohnung §e" + flatToRemove + " §awurde gelöscht");
            }

            case "claim" -> {
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.intersects(player.getLocation())) {
                            OfflinePlayer owner = flatsConfig.getOwner(flat);
                            if (owner == null) {
                                flatsConfig.setOwner(flat, player);
                                player.sendMessage(Flats.PREFIX + "§aHerzlichen Glückwunsch zu deiner neuen Wohnung");
                                return true;
                            }
                            if (owner.getUniqueId().equals(player.getUniqueId())) {
                                player.sendMessage(Flats.PREFIX + "§cBruder, das ist doch schon deine Wohnung");
                                return true;
                            }
                            player.sendMessage(Flats.PREFIX + "§cDie Wohnung gehört schon §6" + owner.getName());
                            return true;
                        }
                    }
                }
                player.sendMessage(Flats.PREFIX + "§cDu befindest dich derzeit in keiner Wohnung");
            }

            case "unclaim" -> {
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.intersects(player.getLocation())) {
                            OfflinePlayer owner = flatsConfig.getOwner(flat);
                            if (owner == null || !owner.getUniqueId().equals(player.getUniqueId())) {
                                player.sendMessage(Flats.PREFIX + "§cDas ist doch gar nicht deine Wohnung");
                                return true;
                            }
                            player.sendMessage(Flats.PREFIX + "§aGlückwunsch zur verlorenen Wohnung");
                            flatsConfig.setOwner(flat, null);
                            return true;
                        }
                    }
                }
                player.sendMessage(Flats.PREFIX + "§cDu befindest dich derzeit in keiner Wohnung");
            }

            case "info" -> {
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.intersects(player.getLocation())) {
                            player.sendMessage(Flats.PREFIX + "§6§lWohnung: §e§l" + flat);
                            OfflinePlayer owner = flatsConfig.getOwner(flat);
                            if (owner == null) {
                                player.sendMessage(Flats.PREFIX + "§7├§6Owner: §eNicht besetzt");
                            } else {
                                player.sendMessage(Flats.PREFIX + "§7├§6Owner: §e" + owner.getName());
                            }
                            player.sendMessage(Flats.PREFIX + "§7└§6Aktuelle Fläche: " + selectionString);
                            return true;
                        }
                    }
                }
                player.sendMessage(Flats.PREFIX + "§cDu befindest dich derzeit in keiner Wohnung");
            }

            case "list" -> {
                if (!Flats.checkPermission(player, Permissions.ADMIN)) {
                    return true;
                }
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    player.sendMessage(Flats.PREFIX + "§6§lWohnung: §e§l" + flat);
                    OfflinePlayer owner = flatsConfig.getOwner(flat);
                    if (owner == null) {
                        player.sendMessage(Flats.PREFIX + "§7├§6Owner: §eNicht besetzt");
                    } else {
                        player.sendMessage(Flats.PREFIX + "§7├§6Owner: §e" + owner.getName());
                    }
                    player.sendMessage(Flats.PREFIX + "§7└§6Flächen:");
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        if (flatsConfig.getAreas(flat).indexOf(selectionString) == flatsConfig.getAreas(flat).size() - 1) {
                            player.sendMessage(Flats.PREFIX + "  §7└§6" + selectionString);
                            break;
                        }
                        player.sendMessage(Flats.PREFIX + "  §7├§6" + selectionString);
                    }
                }
            }

            case "show" -> {
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.getPos1().distance(player.getLocation()) > 100 && selection.getPos2().distance(player.getLocation()) > 100) {
                            continue;
                        }
                        for (Block block : selection.getBlockList()) {
                            player.sendBlockChange(block.getLocation(), Material.YELLOW_STAINED_GLASS.createBlockData());
                            Bukkit.getScheduler().runTaskLater(Flats.getInstance(), () -> player.sendBlockChange(block.getLocation(), block.getLocation().getBlock().getBlockData()), 10 * 20);
                        }
                    }
                }
            }

            default -> sendHelpMessage(player);
        }
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(Flats.PREFIX + "§bVerfügbare Befehle§7:");
        if (player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage("§6/flats select §7- §eAuswahlwerkzeug erhalten");
            player.sendMessage("§6/flats add <wohnungsname> §7- §eWohnung hinzufügen");
            player.sendMessage("§6/flats remove <wohnungsname> §7- §eWohnung entfernen");
            player.sendMessage("§6/flats list §7- §eListe aller Wohnungen anzeigen");
        }
        player.sendMessage("§6/flats claim §7- §eWohnung beanspruchen");
        player.sendMessage("§6/flats unclaim §7- §eWohnung freigeben");
        player.sendMessage("§6/flats info §7- §eInformationen zur aktuellen Wohnung anzeigen");
        player.sendMessage("§6/flats show §7- §eWohnungen in der Nähe anzeigen");
    }

}