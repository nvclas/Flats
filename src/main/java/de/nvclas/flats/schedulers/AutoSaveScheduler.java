package de.nvclas.flats.schedulers;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.SettingsConfig;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

/**
 * Manages the scheduling of periodic auto-save tasks for the plugin's data.
 * <p>
 * The {@code AutoSaveScheduler} runs a repetitive task at a specified interval to save all data
 * managed by the plugin. It provides methods to start and stop this task. The scheduler only starts
 * if the interval is greater than zero.
 */
public class AutoSaveScheduler {

    private final Flats flatsPlugin;
    private final SettingsConfig settingsConfig;
    private final long autoSaveInterval;
    private BukkitTask task;
    private boolean running = false;

    public AutoSaveScheduler(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        autoSaveInterval = settingsConfig.getAutoSaveInterval();
    }

    /**
     * Starts the auto-save scheduler if it is not already running and the auto-save interval is
     * greater than zero. This method schedules a periodic task to save all flats at the configured
     * interval asynchronously.
     * <p>
     * If the scheduler is already running, it throws an {@link UnsupportedOperationException}.
     * If the auto-save interval is zero or negative, auto-saving is disabled, and a log message is
     * recorded.
     *
     * @throws UnsupportedOperationException if the scheduler is already running.
     */
    public void start() {
        if (running) {
            throw new UnsupportedOperationException("AutoSaveScheduler is already running!");
        }
        if (autoSaveInterval <= 0) {
            flatsPlugin.getLogger()
                    .log(Level.INFO, () -> "Auto saving is disabled as autoSaveInterval is below 0 or missing");
            return;
        }

        flatsPlugin.getLogger()
                .log(Level.INFO,
                        () -> "Started AutoSaveScheduler with interval " + settingsConfig.getAutoSaveInterval());
        running = true;
        task = new BukkitRunnable() {

            @Override
            public void run() {
                flatsPlugin.getLogger().log(Level.CONFIG, () -> "Saving flats...");
                flatsPlugin.getFlatsCache().saveAll();
                flatsPlugin.getLogger().log(Level.CONFIG, () -> "Flats saved");
            }
        }.runTaskTimerAsynchronously(flatsPlugin, 0, settingsConfig.getAutoSaveInterval() * 20);
    }

    /**
     * Stops the currently running auto-save task if it is active.
     * <p>
     * This method cancels the ongoing {@link BukkitTask}, if one exists and is not already
     * cancelled, and resets the internal state of the scheduler to indicate it is no longer running.
     */
    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        running = false;
        task = null;
    }
}
