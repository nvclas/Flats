package de.nvclas.flats.schedulers;

import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The {@code CommandDelayScheduler} class manages delayed execution of commands for specific players.
 * It provides methods to start a delay, retrieve remaining delay time, and stop all active delays.
 * <p>
 * Each instance associates a command with a delay in ticks and schedules a task to decrement the delay over time.
 */
@Getter
public class CommandDelayScheduler {

    private static final Map<CommandDelayScheduler, UUID> delays = new HashMap<>();
    private final String command;
    private long delay;
    private BukkitTask task;

    public CommandDelayScheduler(String command, long delay) {
        this.command = command;
        this.delay = delay;
    }

    /**
     * Retrieves the remaining delay for a specified command associated with a player.
     * <p>
     * If no delay exists for the given player and command, returns 0.
     *
     * @param player  The {@link OfflinePlayer} for whom the delay is being checked.
     * @param command The command whose delay is being retrieved.
     * @return The remaining delay in ticks, or 0 if no delay is set for the given player and command.
     */
    public static long getDelay(OfflinePlayer player, String command) {
        UUID playerId = player.getUniqueId();
        return delays.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(playerId) && entry.getKey().getCommand().equals(command))
                .map(entry -> entry.getKey().getDelay())
                .findFirst()
                .orElse(0L);
    }

    /**
     * Cancels all currently active command delay tasks managed by {@code CommandDelayScheduler}.
     * <p>
     * This method iterates through all running tasks stored in the {@code delays} map and cancels them,
     * effectively stopping all scheduled command delays.
     */
    public static void stopAll() {
        delays.keySet().forEach(scheduler -> scheduler.task.cancel());
    }

    /**
     * Starts the command delay scheduler for the specified player and plugin.
     * <p>
     * This method initializes a delay countdown for the associated command and player. It schedules
     * a task that decrements the delay periodically and removes the delay entry when it reaches zero.
     *
     * @param player the {@link OfflinePlayer} for whom the command delay is being scheduled.
     * @param plugin the {@link JavaPlugin} instance required to schedule the task asynchronously.
     */
    public void start(OfflinePlayer player, JavaPlugin plugin) {
        delays.put(this, player.getUniqueId());

        task = new BukkitRunnable() {
            @Override
            public void run() {
                delay--;
                delays.put(CommandDelayScheduler.this, player.getUniqueId());
                if (delay <= 0) {
                    delays.remove(CommandDelayScheduler.this);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20);
    }

}
