package de.nvclas.flats.commands.flats.subcommands;

import de.nvclas.flats.Flats;
import de.nvclas.flats.cache.FlatsCache;
import de.nvclas.flats.commands.flats.SubCommand;
import de.nvclas.flats.config.SettingsConfig;
import de.nvclas.flats.util.CommandUtils;
import de.nvclas.flats.util.I18n;
import de.nvclas.flats.util.Permissions;
import de.nvclas.flats.util.ValidationUtils;
import de.nvclas.flats.volumes.Flat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RenameSubCommand implements SubCommand {

    private final SettingsConfig settingsConfig;
    private final FlatsCache flatsCache;

    public RenameSubCommand(Flats flatsPlugin) {
        this.settingsConfig = flatsPlugin.getSettingsConfig();
        this.flatsCache = flatsPlugin.getFlatsCache();
    }

    @Override
    public void execute(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (CommandUtils.isCommandOnCooldown(player, "flats rename")) {
            return;
        }

        if (!Permissions.canClaimFlats(player, settingsConfig)) {
            Permissions.showNoPermissionMessage(player);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(Flats.PREFIX + I18n.translate("rename.usage"));
            return;
        }

        String oldName = args[1];
        String newName = args[2];

        if (!ValidationUtils.isValidFlatName(player, newName)) {
            String suggested = ValidationUtils.suggestValidName(newName);
            player.sendMessage(Flats.PREFIX + I18n.translate("validation.suggestion", suggested));
            return;
        }

        if (!flatsCache.existsFlat(oldName)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.flat_not_exist"));
            return;
        }

        if (flatsCache.existsFlat(newName)) {
            player.sendMessage(Flats.PREFIX + I18n.translate("rename.name_taken", newName));
            return;
        }

        Flat flat = flatsCache.getExistingFlat(oldName);
        if (flat.getOwner() == null || !player.getUniqueId().equals(flat.getOwner().getUniqueId())) {
            player.sendMessage(Flats.PREFIX + I18n.translate("error.not_your_flat"));
            return;
        }

        // Rename the flat by updating the name and recreating it in the cache
        flat.setName(newName);
        flatsCache.delete(oldName);
        
        // Create new flat with new name but preserve all data
        flatsCache.create(newName, flat.getAreas().get(0));
        Flat newFlat = flatsCache.getExistingFlat(newName);
        newFlat.setOwner(flat.getOwner());
        newFlat.getTrusted().clear();
        newFlat.getTrusted().addAll(flat.getTrusted());
        
        // Add remaining areas if there are more than one
        for (int i = 1; i < flat.getAreas().size(); i++) {
            newFlat.getAreas().add(flat.getAreas().get(i));
        }

        player.sendMessage(Flats.PREFIX + I18n.translate("rename.success", oldName, newName));
    }
}