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
 * Manages delayed execution of commands linked to specific players.
 * <p>
 * This class allows scheduling and tracking delays associated with specific commands for individual players.
 * It utilizes a {@link BukkitRunnable} to manage asynchronous decrementing of delays and, upon expiration,
 * removes the corresponding command delay entry from the internal map.
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
     * Retrieves the delay associated with a particular command for a specified player.
     * <p>
     * This method checks the {@code delays} map for entries matching the given player's unique identifier
     * and the specified command. If an entry is found, the corresponding delay value is returned.
     * If no match is found, the method returns {@code 0L}.
     *
     * @param player  The {@link OfflinePlayer} for whom the delay is being retrieved.
     *                This is used to identify the relevant entry in the {@code delays} map.
     * @param command The specific command whose delay is being checked.
     *                This is used to filter entries in the {@code delays} map.
     * @return The delay value, in ticks, associated with the specified player and command.
     * Returns {@code 0L} if no matching entry is found.
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
     * Stops all scheduled command delays.
     * <p>
     * This method clears the internal {@code delays} map, effectively removing all entries
     * and halting all currently pending command delays.
     */
    public static void stopAll() {
        delays.keySet().forEach(scheduler -> scheduler.task.cancel());
    }

    /**
     * Starts a scheduled command delay for the given {@link OfflinePlayer}.
     * <p>
     * This method associates the {@link CommandDelayScheduler} instance with the player's unique identifier
     * in the internal {@code delays} map, and initializes a {@link BukkitRunnable} to decrement the
     * delay time asynchronously. Once the delay reaches zero, the association is removed.
     *
     * @param player The {@link OfflinePlayer} for whom the command delay is being scheduled. The mapping
     *               between this instance and the player's unique identifier is stored to track active delays.
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
