package de.nvclas.flats.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SelectionItem {

    private static final String DISPLAY_NAME = "Selection";
    private static final Material MATERIAL = Material.STICK;

    private SelectionItem() {
        throw new IllegalStateException("Utility class");
    }

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
