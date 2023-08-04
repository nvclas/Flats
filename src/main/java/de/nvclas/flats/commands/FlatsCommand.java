package de.nvclas.flats.commands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.FlatsConfig;
import de.nvclas.flats.items.SelectionItem;
import de.nvclas.flats.utils.LocationConverter;
import de.nvclas.flats.utils.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlatsCommand implements CommandExecutor {

    private final FlatsConfig flatsConfig = Flats.getInstance().getFlatsConfig();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("flat")) {
            return true;
        }
        if (!(sender instanceof Player p)) {
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(Flats.PREFIX + "§cBruder, was willst du?");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "select" -> {
                if (!Flats.checkPermission(p, "flats.admin")) {
                    return true;
                }
                p.getInventory().addItem(SelectionItem.getItem());
            }

            case "add" -> {
                if (!Flats.checkPermission(p, "flats.admin")) {
                    return true;
                }
                if (Selection.getSelection(p).calculateVolume() == 0) {
                    p.sendMessage(Flats.PREFIX + "§cBruder, du hast nichts markiert!");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage(Flats.PREFIX + "§cNutze §6/flats add <wohnungsname>");
                    return true;
                }
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.intersects(Selection.getSelection(p))) {
                            p.sendMessage(Flats.PREFIX + "§cDeine Auswahl schneidet eine andere Wohnung");
                            p.sendMessage(Flats.PREFIX + "§cWohnung: §6" + flat + " §cbei §6" + selectionString);
                            return true;
                        }
                    }
                }
                String flatName = args[1];
                flatsConfig.addSelection(flatName, Selection.getSelection(p));
                p.sendMessage(Flats.PREFIX + "§aWohnung §e" + flatName + " §awurde erstellt");
            }

            case "remove" -> {
                if (!Flats.checkPermission(p, "flats.admin")) {
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage(Flats.PREFIX + "§cNutze §6/flats remove <wohnungsnahme>");
                    return true;
                }
                String flatToRemove = args[1];
                if (!flatsConfig.getConfig().isSet(flatToRemove)) {
                    p.sendMessage(Flats.PREFIX + "§cBruder, diese Wohnung gibt es nicht");
                    return true;
                }
                flatsConfig.getConfig().set(flatToRemove, null);
                flatsConfig.saveConfig();
                p.sendMessage(Flats.PREFIX + "§aWohnung §e" + flatToRemove + " §awurde gelöscht");
            }

            case "claim" -> {
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.intersects(p.getLocation())) {
                            OfflinePlayer owner = flatsConfig.getOwner(flat);
                            if (owner == null) {
                                flatsConfig.setOwner(flat, p);
                                p.sendMessage(Flats.PREFIX + "§aHerzlichen Glückwunsch zu deiner neuen Wohnung");
                                return true;
                            }
                            if (owner.getUniqueId().equals(p.getUniqueId())) {
                                p.sendMessage(Flats.PREFIX + "§cBruder, das ist doch schon deine Wohnung");
                                return true;
                            }
                            p.sendMessage(Flats.PREFIX + "§cDie Wohnung gehört schon §6" + owner.getName());
                            return true;
                        }
                    }
                }
                p.sendMessage(Flats.PREFIX + "§cDu befindest dich derzeit in keiner Wohnung");
            }

            case "unclaim" -> {
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.intersects(p.getLocation())) {
                            OfflinePlayer owner = flatsConfig.getOwner(flat);
                            if (owner == null || !owner.getUniqueId().equals(p.getUniqueId())) {
                                p.sendMessage(Flats.PREFIX + "§cDas ist doch gar nicht deine Wohnung");
                                return true;
                            }
                            p.sendMessage(Flats.PREFIX + "§aGlückwunsch zur verlorenen Wohnung");
                            flatsConfig.setOwner(flat, null);
                            return true;
                        }
                    }
                }
                p.sendMessage(Flats.PREFIX + "§cDu befindest dich derzeit in keiner Wohnung");
            }

            case "info" -> {
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.intersects(p.getLocation())) {
                            p.sendMessage(Flats.PREFIX + "§6§lWohnung: §e§l" + flat);
                            OfflinePlayer owner = flatsConfig.getOwner(flat);
                            if (owner == null) {
                                p.sendMessage(Flats.PREFIX + "§7├§6Owner: §eNicht besetzt");
                            } else {
                                p.sendMessage(Flats.PREFIX + "§7├§6Owner: §e" + owner.getName());
                            }
                            p.sendMessage(Flats.PREFIX + "§7└§6Aktuelle Fläche: " + selectionString);
                            return true;
                        }
                    }
                }
                p.sendMessage(Flats.PREFIX + "§cDu befindest dich derzeit in keiner Wohnung");
            }

            case "list" -> {
                if (!Flats.checkPermission(p, "flats.admin")) {
                    return true;
                }
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    p.sendMessage(Flats.PREFIX + "§6§lWohnung: §e§l" + flat);
                    OfflinePlayer owner = flatsConfig.getOwner(flat);
                    if (owner == null) {
                        p.sendMessage(Flats.PREFIX + "§7├§6Owner: §eNicht besetzt");
                    } else {
                        p.sendMessage(Flats.PREFIX + "§7├§6Owner: §e" + owner.getName());
                    }
                    p.sendMessage(Flats.PREFIX + "§7└§6Flächen:");
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        if (flatsConfig.getAreas(flat).indexOf(selectionString) == flatsConfig.getAreas(flat).size() - 1) {
                            p.sendMessage(Flats.PREFIX + "  §7└§6" + selectionString);
                            break;
                        }
                        p.sendMessage(Flats.PREFIX + "  §7├§6" + selectionString);
                    }
                }
            }

            case "show" -> {
                for (String flat : flatsConfig.getConfig().getKeys(false)) {
                    for (String selectionString : flatsConfig.getAreas(flat)) {
                        Selection selection = LocationConverter.getSelectionFromString(selectionString);
                        if (selection.getPos1().distance(p.getLocation()) > 30 && selection.getPos2().distance(p.getLocation()) > 30) {
                            p.sendMessage(flat + ": Nicht in der Nähe");
                        }
                        for (Block block : selection.getBlockList()) {
                            p.sendBlockChange(block.getLocation(), Material.YELLOW_STAINED_GLASS.createBlockData());
                            Bukkit.getScheduler().runTaskLater(Flats.getInstance(), () -> {
                                p.sendBlockChange(block.getLocation(), block.getLocation().getBlock().getBlockData());
                            }, 10 * 20);
                        }
                    }
                }
            }

            default -> p.sendMessage(Flats.PREFIX + "§cKenn ich nicht");
        }
        return true;
    }
}