package de.nvclas.flats.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SelectionItem {

    private static final String DISPLAY_NAME = "§6Selection";
    private static final Material MATERIAL = Material.STICK;

    private SelectionItem() {
        throw new IllegalStateException("Utility class");
    }
    
    public static ItemStack getItem() {
        ItemStack is = new ItemStack(MATERIAL);
        ItemMeta im = is.getItemMeta();
        if(im != null) {
            im.setDisplayName(DISPLAY_NAME);
            is.setItemMeta(im);
        }
        return is;
    }

}
