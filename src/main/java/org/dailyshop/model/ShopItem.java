package org.dailyshop.model;

import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.dailyshop.utils.ItemGroups;

import java.util.List;

public class ShopItem {
    private final Material material;   // null si custom
    private final String customId;     // ex : "customcrops:tomato"
    private final double price;

    public ShopItem(Material material, String customId, double price) {
        this.material = material;
        this.customId = customId;
        this.price    = price;
    }

    public boolean isCustom() {
        return customId != null && !customId.isBlank();
    }
    public Material getMaterial()   { return material; }
    public String getCustomId()     { return customId; }
    public double getPrice()        { return price; }

    /**
     * Retourne true si cet ItemStack correspond à ce ShopItem,
     * en tenant compte des variants (laine, froglight, etc.).
     */
    public boolean matches(ItemStack stack) {
        if (stack == null) return false;

        if (isCustom()) {
            // custom ItemsAdder
            return ItemsAdder.isCustomItem(stack)
                    && ItemsAdder.matchCustomItemName(stack, customId);
        } else {
            Material clicked = stack.getType();
            // 1) correspondance directe
            if (clicked == material) return true;

            // 2) correspondance via groupe de variants
            List<Material> variants = ItemGroups.VARIANTS.get(material);
            return variants != null && variants.contains(clicked);
        }
    }
}
