package de.nvclas.flats.items;

import de.nvclas.flats.Flats;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class for managing the creation of a preconfigured selection item.
 * <p>
 * The {@code SelectionItem} provides a custom item with a specific material and
 * display name styling for use in selection-related functionality.
 */
@UtilityClass
public class SelectionItem {

    private static final String DISPLAY_NAME = "Selection";
    private static final Material MATERIAL = Material.STICK;
    private static final String KEY_IDENTIFIER = "selection_item";
    private static final NamespacedKey SELECTION_KEY = new NamespacedKey(JavaPlugin.getPlugin(Flats.class),
            KEY_IDENTIFIER);

    /**
     * Creates and returns a preconfigured {@link ItemStack} representing the selection item.
     * The item has a specific material, gold-colored display name, and disabled italic style.
     *
     * @return a non-null {@link ItemStack} representing the selection tool used in various commands or events.
     */
    public static @NotNull ItemStack getItem() {
        ItemStack is = new ItemStack(MATERIAL);
        is.editMeta(im -> {
            im.displayName(Component.text(DISPLAY_NAME)
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)
            );
            im.getPersistentDataContainer().set(SELECTION_KEY, PersistentDataType.BOOLEAN, true);
            im.setMaxStackSize(1);
        });
        return is;
    }

}
