package de.nvclas.flats.schedulers;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.managers.FlatsManager;
import lombok.experimental.UtilityClass;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * The {@code AutoSaveScheduler} is a utility class responsible for managing periodic auto-save tasks
 * in the Flats plugin. It utilizes the {@link BukkitRunnable} to schedule tasks at fixed intervals
 * defined in the plugin's {@link SettingsConfig}.
 * <p>
 * The scheduler automatically saves all flats using {@link FlatsManager#saveAll} at the specified
 * auto-save interval. It also provides methods to start and stop the scheduler, ensuring that only
 * one instance of the scheduler can run at a time.
 */
@UtilityClass
public class AutoSaveScheduler {

    private static final SettingsConfig settingsConfig = Flats.getInstance().getSettingsConfig();
    private static final JavaPlugin plugin = Flats.getInstance();
    private static final long AUTO_SAVE_INTERVAL = settingsConfig.getAutoSaveInterval();
    private static BukkitTask task;
    private static boolean running = false;

    /**
     * Starts the AutoSaveScheduler, which periodically saves all flats using {@link FlatsManager#saveAll}.
     * <p>
     * This method initializes and schedules a {@link BukkitRunnable} task to execute at regular intervals,
     * defined by the auto-save interval retrieved from {@link SettingsConfig#getAutoSaveInterval}.
     * If the scheduler is already running, an {@link UnsupportedOperationException} will be thrown.
     * <p>
     * Once started, the scheduler logs its initiation and subsequent periodic save events to the plugin's logger.
     *
     * @throws UnsupportedOperationException if the AutoSaveScheduler is already running.
     */
    public static void start() {
        if (running) {
            throw new UnsupportedOperationException("AutoSaveScheduler is already running!");
        }
        if (AUTO_SAVE_INTERVAL <= 0) {
            plugin.getLogger().info("Auto saving is disabled as autoSaveInterval is below 0 or missing");
            return;
        }

        plugin.getLogger().info("Started AutoSaveScheduler with interval " + settingsConfig.getAutoSaveInterval());
        running = true;
        task = new BukkitRunnable() {

            @Override
            public void run() {
                plugin.getLogger().info("Saving flats...");
                FlatsManager.saveAll();
                plugin.getLogger().info("Flats saved.");
            }
        }.runTaskTimerAsynchronously(plugin, 0, settingsConfig.getAutoSaveInterval() * 20);
    }

    /**
     * Stops the AutoSaveScheduler.
     * <p>
     * This method cancels the currently running {@link BukkitTask} if it exists and has not been cancelled yet,
     * effectively halting the periodic auto-save operations. Once stopped, the {@code running} flag is set to {@code false},
     * and the {@code task} reference is set to {@code null}.
     */
    public static void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        running = false;
        task = null;
    }
}
