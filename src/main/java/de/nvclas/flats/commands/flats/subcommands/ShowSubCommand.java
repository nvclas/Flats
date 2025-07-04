package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.commands.flats.FlatsSubCommand;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.schedulers.CommandDelayScheduler;
import de.nvclas.flats.util.CommandUtils;
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

    private static final byte DEFAULT_SHOW_TIME = 10;
    private static final int MAX_UPDATES_PER_TICK = 100;
    private static final double MAX_DISTANCE = 100.0;
    private static final long SCHEDULER_DELAY = 0L;
    private static final long SCHEDULER_PERIOD = 1L;

    private final Flats flatsPlugin;

    public ShowSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!player.hasPermission(Permissions.ADMIN)) {
            new CommandDelayScheduler(FlatsSubCommand.SHOW.getFullCommandName(), DEFAULT_SHOW_TIME)
                    .start(player, flatsPlugin);
        }

        if (CommandUtils.isCommandOnCooldown(player, FlatsSubCommand.SHOW.getFullCommandName())) {
            return;
        }

        player.sendMessage(Flats.PREFIX + I18n.translate("show.success", DEFAULT_SHOW_TIME));

        List<Block> blocksToChange = getBlocksToChange(player);
        scheduleBlockUpdates(player, blocksToChange);
    }

    private void scheduleBlockUpdates(@NotNull Player player, @NotNull List<Block> blocksToChange) {
        new BukkitRunnable() {
            private int currentIndex = 0;

            @Override
            public void run() {
                if (currentIndex >= blocksToChange.size()) {
                    cancel();
                    scheduleBlockRestore(player, blocksToChange);
                    return;
                }

                updateBlocksBatch(player, blocksToChange);
            }

            private void updateBlocksBatch(@NotNull Player player, @NotNull List<Block> blocksToChange) {
                int endIndex = Math.min(blocksToChange.size(), currentIndex + MAX_UPDATES_PER_TICK);
                for (int i = currentIndex; i < endIndex; i++) {
                    Block block = blocksToChange.get(i);
                    player.sendBlockChange(block.getLocation(), Material.YELLOW_STAINED_GLASS.createBlockData());
                }
                currentIndex = endIndex;
            }
        }.runTaskTimer(flatsPlugin, SCHEDULER_DELAY, SCHEDULER_PERIOD);
    }

    private void scheduleBlockRestore(@NotNull Player player, @NotNull List<Block> blocksToChange) {
        Bukkit.getScheduler().runTaskLater(flatsPlugin,
                () -> player.sendBlockChanges(blocksToChange.stream()
                        .map(Block::getState)
                        .toList()),
                20L * DEFAULT_SHOW_TIME);
    }

    private @NotNull List<Block> getBlocksToChange(@NotNull Player player) {
        List<Block> blocksToChange = new ArrayList<>();

        flatsPlugin.getFlatsCache()
                .getAllAreas()
                .stream()
                .filter(area -> area.isWithinDistance(player.getLocation(), MAX_DISTANCE))
                .forEach(area -> blocksToChange.addAll(area.getAllOuterBlocks()));

        return blocksToChange;
    }
}
