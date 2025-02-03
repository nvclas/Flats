package de.nvclas.flats.items;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
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

    /**
     * Creates and returns an {@link ItemStack} representing the selection item with a custom display name and formatting.
     * The item is configured with a material and its name is styled in gold with italics disabled.
     *
     * @return A non-null {@link ItemStack} instance representing the pre-configured selection item.
     */
    public static @NotNull ItemStack getItem() {
        ItemStack is = new ItemStack(MATERIAL);
        is.editMeta(im -> im.displayName(
                Component.text(DISPLAY_NAME)
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        return is;
    }

}
