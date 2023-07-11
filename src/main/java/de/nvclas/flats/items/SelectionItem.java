package de.nvclas.flats.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SelectionItem {

    private static final String displayName = "§6Selection";
    private static final Material material = Material.STICK;

    public static ItemStack getItem() {
        ItemStack is = new ItemStack(material);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(displayName);
        is.setItemMeta(im);
        return is;
    }

}
