package org.dailyshop.listeners;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.model.ShopItem;
import org.dailyshop.utils.SellChestItem;

import java.util.Map;

public class SellChestListener implements Listener {

    private final DailyShopPlugin plugin;

    public SellChestListener(DailyShopPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRightClickChest(PlayerInteractEvent event) {
        // clic-droit main principale uniquement
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || !(clicked.getState() instanceof Container container)) return;

        Player player = event.getPlayer();
        ItemStack stick = player.getInventory().getItemInMainHand();
        if (!SellChestItem.isSellChestItem(stick)) return;

        // Vérifie claim GriefPrevention
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(clicked.getLocation(), false, null);
        if (claim == null) {
            player.sendMessage("§cCe coffre n'est pas protégé – impossible de vendre ici.");
            return;
        }

        String denial = claim.allowContainers(player);
        if (denial != null) {
            player.sendMessage("§cVous devez être propriétaire ou avoir la permission container-trust ici.");
            return;
        }

        event.setCancelled(true);

        // Shop actif uniquement
        Shop shop = plugin.getShopManager().getCurrentShop();
        if (shop == null) {
            player.sendMessage("§cAucun shop actif pour vendre.");
            return;
        }

        // Récupère les items vendables et leur quantité
        Map<ShopItem, Integer> soldItems = plugin.getSellManager().collectSellableItems(container.getInventory(), shop);
        if (soldItems.isEmpty()) {
            player.sendMessage("§eAucun item vendable dans ce coffre.");
            return;
        }

        // Paiement + enregistrement
        plugin.getSellManager().payAndRecord(player, soldItems);

        // Feedback visuel
        player.sendMessage("§aVous avez vendu les items de ce coffre avec votre §3SellStick§a !");
        player.playSound(player.getLocation(), "minecraft:entity.player.levelup", 1f, 1.2f);

        // Gérer les utilisations du SellStick
        int uses = SellChestItem.getRemainingUses(stick);
        if (uses > 1) {
            SellChestItem.decreaseUses(stick);
        } else {
            player.getInventory().setItemInMainHand(null);
            player.sendMessage("§cVotre SellStick est usé !");
        }
    }
}
