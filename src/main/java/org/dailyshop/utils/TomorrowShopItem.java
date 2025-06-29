package org.dailyshop.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TomorrowShopItem {

    public static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dOeil de cristal");
            meta.setLore(List.of(
                    "§7Clic droit pour prévisualiser le marchand de demain.",
                    "§cUtilisation unique"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isTomorrowShopItem(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_EYE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && "§dOeil de cristal".equals(meta.getDisplayName());
    }

}
