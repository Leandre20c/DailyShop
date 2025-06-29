package org.dailyshop.managers;

import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.dailyshop.model.Shop;
import org.dailyshop.model.ShopItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellManager {

    /**
     * Retourne une map des ShopItem vendus et leur quantité, en retirant les items du tableau donné
     * @param inventory Contenu du coffre ou inventaire
     * @param shop Shop de référence pour connaître les prix
     * @return map de ShopItem → quantité vendue
     */
    public Map<ShopItem, Integer> sellItems(ItemStack[] inventory, Shop shop) {
        Map<ShopItem, Integer> result = new HashMap<>();
        List<ShopItem> shopItems = shop.getItems();

        for (int i = 0; i < inventory.length; i++) {
            ItemStack item = inventory[i];
            if (item == null) continue;

            for (ShopItem shopItem : shopItems) {
                if (shopItem.getMaterial() == item.getType()) {
                    int quantity = item.getAmount();
                    result.put(shopItem, result.getOrDefault(shopItem, 0) + quantity);
                    inventory[i] = null; // Retirer l'item
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Calcule le total en $ d'une vente donnée
     */
    public double calculateTotal(Map<ShopItem, Integer> sold) {
        double total = 0;
        for (Map.Entry<ShopItem, Integer> entry : sold.entrySet()) {
            total += entry.getKey().getPrice() * entry.getValue();
        }
        return total;
    }

    /** Parcourt chaque slot, vend les stacks matching ShopItem (vanilla ou IA), retourne le total encaissé. */
    public double sellContainer(Inventory inv, Shop shop) {
        double total = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null) continue;

            for (ShopItem sItem : shop.getItems()) {
                boolean match = false;

                if (sItem.isCustom()) {
                    // ItemsAdder custom item ?
                    if (ItemsAdder.isCustomItem(stack)
                            && ItemsAdder.matchCustomItemName(stack, sItem.getCustomId())) {
                        match = true;
                    }
                } else {
                    // item “vanilla”
                    if (stack.getType() == sItem.getMaterial()) {
                        match = true;
                    }
                }

                if (match) {
                    int amount = stack.getAmount();
                    total += sItem.getPrice() * amount;
                    inv.setItem(i, null);  // vend et supprime le stack
                    break;
                }
            }
        }

        return total;
    }
}
