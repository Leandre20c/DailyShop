package org.dailyshop.model;

import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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
     * que ce soit un vanilla ou un custom ItemsAdder.
     */
    public boolean matches(ItemStack stack) {
        if (stack == null) return false;
        if (isCustom()) {
            // ItemsAdder API
            return ItemsAdder.isCustomItem(stack)
                    && ItemsAdder.matchCustomItemName(stack, customId);
        } else {
            return stack.getType() == material;
        }
    }
}
