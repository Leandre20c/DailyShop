package org.dailyshop.anim;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.dailyshop.model.ShopItem;
import dev.lone.itemsadder.api.CustomStack;

import java.util.List;

public class AnimatedSlot {
    private final int slotIndex;
    private final ShopItem shopItem;
    private final List<Material> variants;
    private int current = 0;

    public AnimatedSlot(int slot, ShopItem item, List<Material> variants) {
        this.slotIndex = slot;
        this.shopItem  = item;
        this.variants  = variants;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    /** Passe à la variante suivante, en bouclant. */
    public void advance() {
        current = (current + 1) % variants.size();
    }

    /** Construit l’ItemStack à placer dans l’inventaire pour cette variante. */
    public ItemStack buildStack() {
        Material mat = variants.get(current);
        ItemStack item;
        if (shopItem.isCustom()) {
            // customItems ne sont pas animés ici, mais on garde la possibilité
            CustomStack cs = CustomStack.getInstance(shopItem.getCustomId());
            item = cs != null ? cs.getItemStack() : new ItemStack(mat);
        } else {
            item = new ItemStack(mat);
        }
        var meta = item.getItemMeta();
        if (meta != null) {
            String display = shopItem.isCustom()
                    ? shopItem.getCustomId().split(":",2)[1]
                    : formatMaterialName(mat);
            meta.setDisplayName("§f" + display);
            meta.setLore(List.of("§7Prix: §a" + shopItem.getPrice() + " PE"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatMaterialName(Material m) {
        String name = m.name().toLowerCase().replace("_", " ");
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
