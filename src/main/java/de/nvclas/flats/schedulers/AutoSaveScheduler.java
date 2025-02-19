package de.nvclas.flats.schedulers;

import de.nvclas.flats.Flats;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.managers.FlatsManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

/**
 * The {@code AutoSaveScheduler} is a utility class responsible for managing periodic auto-save tasks
 * in the Flats plugin. It utilizes the {@link BukkitRunnable} to schedule tasks at fixed intervals
 * defined in the plugin's {@link SettingsConfig}.
 * <p>
 * The scheduler automatically saves all flats using {@link FlatsManager#saveAll} at the specified
 * auto-save interval. It also provides methods to start and stop the scheduler, ensuring that only
 * one instance of the scheduler can run at a time.
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
                flatsPlugin.getFlatsManager().saveAll();
                flatsPlugin.getLogger().log(Level.CONFIG, () -> "Flats saved");
            }
        }.runTaskTimerAsynchronously(flatsPlugin, 0, settingsConfig.getAutoSaveInterval() * 20);
    }

    /**
     * Stops the AutoSaveScheduler.
     * <p>
     * This method cancels the currently running {@link BukkitTask} if it exists and has not been cancelled yet,
     * effectively halting the periodic auto-save operations. Once stopped, the {@code running} flag is set to {@code false},
     * and the {@code task} reference is set to {@code null}.
     */
    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        running = false;
        task = null;
    }
}
