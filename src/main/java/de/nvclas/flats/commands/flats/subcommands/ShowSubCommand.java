package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.FlatsSubCommand;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.schedulers.CommandDelayScheduler;
import de.nvclas.flats.util.CommandUtils;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.volumes.Area;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class ShowSubCommand implements SubCommand {

    private static final byte DEFAULT_SHOW_TIME = 10;
    private static final int MAX_DISTANCE = 100;
    private static final float EDGE_THICKNESS = 0.08f;
    private static final BlockData FRAME_BLOCK_DATA = Material.YELLOW_STAINED_GLASS.createBlockData();

    private final Flats flatsPlugin;
    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public ShowSubCommand(Flats flatsPlugin) {
        this.flatsPlugin = flatsPlugin;
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!Permissions.canShowFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }

        if (CommandUtils.isCommandOnCooldown(player, FlatsSubCommand.SHOW.getFullCommandName())) {
            return;
        }

        if (!Permissions.canSkipCommandDelay(player, settingsConfig)) {
            new CommandDelayScheduler(FlatsSubCommand.SHOW.getFullCommandName(), DEFAULT_SHOW_TIME).start(player,
                    flatsPlugin);
        }

        List<Area> nearbyAreas = getNearbyAreas(player);

        long flatsAmount = nearbyAreas.stream()
                .map(Area::getFlatName)
                .distinct()
                .count();

        if (flatsAmount == 0) {
            player.sendMessage(Flats.PREFIX + I18n.translate("show.none"));
            return;
        }

        if (flatsAmount == 1) {
            player.sendMessage(Flats.PREFIX + I18n.translate("show.success.singular", DEFAULT_SHOW_TIME));
        } else {
            player.sendMessage(Flats.PREFIX + I18n.translate("show.success.plural", flatsAmount, DEFAULT_SHOW_TIME));
        }

        List<Area.Edge> edgesToShow = nearbyAreas.stream()
                .flatMap(area -> area.getEdges().stream())
                .toList();
        showFrames(player, edgesToShow);
    }

    private @NotNull List<Area> getNearbyAreas(@NotNull Player player) {
        int minX = player.getLocation().getBlockX() - MAX_DISTANCE;
        int maxX = player.getLocation().getBlockX() + MAX_DISTANCE;
        int minZ = player.getLocation().getBlockZ() - MAX_DISTANCE;
        int maxZ = player.getLocation().getBlockZ() + MAX_DISTANCE;

        return flatsCache.getAreasIntersecting(player.getWorld().getName(), minX, maxX, minZ, maxZ)
                .stream()
                .filter(area -> area.isWithinDistance(player.getLocation(), MAX_DISTANCE))
                .toList();
    }

    /**
     * Spawns one thin, stretched {@link BlockDisplay} per edge to draw the cube frame.
     * Display entities render at normal entity view distance (far better than particles),
     * have no collision, and are made visible only to the requesting player.
     */
    private void showFrames(@NotNull Player player, @NotNull List<Area.Edge> edges) {
        List<Entity> spawnedEntities = edges.stream()
                .map(edge -> spawnEdgeDisplay(player, edge.start(), edge.end()))
                .map(Entity.class::cast)
                .toList();

        Bukkit.getScheduler().runTaskLater(flatsPlugin,
                () -> spawnedEntities.forEach(Entity::remove),
                20L * DEFAULT_SHOW_TIME);
    }

    private @NotNull BlockDisplay spawnEdgeDisplay(@NotNull Player player, @NotNull Location start,
            @NotNull Location end) {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();
        float length = (float) Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));

        float scaleX = dx != 0 ? length : EDGE_THICKNESS;
        float scaleY = dy != 0 ? length : EDGE_THICKNESS;
        float scaleZ = dz != 0 ? length : EDGE_THICKNESS;

        float offsetX = dx != 0 ? 0f : -EDGE_THICKNESS / 2f;
        float offsetY = dy != 0 ? 0f : -EDGE_THICKNESS / 2f;
        float offsetZ = dz != 0 ? 0f : -EDGE_THICKNESS / 2f;

        Location origin = new Location(start.getWorld(),
                Math.min(start.getX(), end.getX()),
                Math.min(start.getY(), end.getY()),
                Math.min(start.getZ(), end.getZ()));

        Transformation transformation = new Transformation(
                new Vector3f(offsetX, offsetY, offsetZ),
                new Quaternionf(),
                new Vector3f(scaleX, scaleY, scaleZ),
                new Quaternionf());

        BlockDisplay display = origin.getWorld().spawn(origin, BlockDisplay.class, entity -> {
            entity.setBlock(FRAME_BLOCK_DATA);
            entity.setTransformation(transformation);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setShadowRadius(0);
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
        });

        player.showEntity(flatsPlugin, display);
        return display;
    }
}
