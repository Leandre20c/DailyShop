package org.dailyshop.managers;

import dev.lone.itemsadder.api.ItemsAdder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.model.ShopItem;
import org.simpleskills.api.SkillsAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellManager {

    private final Economy economy;
    private final DailyShopPlugin plugin;

    public SellManager(DailyShopPlugin plugin, Economy economy){
        this.economy = economy;
        this.plugin = plugin;
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

    public void payAndRecord(Player player, Map<ShopItem, Integer> sold) {
        double total = calculateTotal(sold);
        if (total <= 0) return;

        // paiement
        double skillBoost = applySkillsBoost(player, total);
        economy.depositPlayer(player, total);
        economy.depositPlayer(player, skillBoost);

        // enregistrement des ventes -> stats
        for (Map.Entry<ShopItem, Integer> entry : sold.entrySet()) {
            ShopItem item = entry.getKey();
            int quantity = entry.getValue();
            double price = item.getPrice() * quantity;
            String itemId = item.isCustom()
                    ? item.getCustomId()
                    : item.getMaterial().name();
            plugin.getStatsManager().recordSale(player.getName(), itemId, quantity, price);
        }

        // message joueur
        if (skillBoost > 0){
            player.sendMessage("§aVous avez vendu pour §e" + total + " PE &a+ §e" + skillBoost + " PE §agrâce aux skills.");
        }
        else {
            player.sendMessage("§aVous avez vendu pour §e" + Math.round(total * 10.0) / 10.0 + " PE");
        }
    }

    public Map<ShopItem, Integer> collectSellableItems(Inventory inv, Shop shop) {
        Map<ShopItem, Integer> result = new HashMap<>();

        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            for (ShopItem shopItem : shop.getItems()) {
                if (shopItem.matches(item)) {
                    int amount = item.getAmount();
                    result.merge(shopItem, amount, Integer::sum);
                    inv.remove(item); // retire directement du coffre
                    break;
                }
            }
        }

        return result;
    }

    public double applySkillsBoost(Player player, double sold){
        int skillBoostLevel = SkillsAPI.getLevel(player, "wealth");
        if (!SkillsAPI.isSkillEnabled(player, "wealth")){
            return 0;
        }

        return (sold * (skillBoostLevel * 0.005));
    }

}
