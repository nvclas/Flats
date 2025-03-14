package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.FlatsSubCommand;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.schedulers.CommandDelayScheduler;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ShowSubCommand implements SubCommand {
    
    private final Flats flatsPlugin;
    
    public ShowSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }
    
    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
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

    private @NotNull List<Block> getBlocksToChange(Player player) {
        List<Block> blocksToChange = new ArrayList<>();

        flatsPlugin.getFlatsManager()
                .getAllAreas()
                .stream()
                .filter(area -> area.isWithinDistance(player.getLocation(), 100))
                .forEach(area -> blocksToChange.addAll(area.getAllOuterBlocks()));

        return blocksToChange;
    }
    
}
