package org.dailyshop.listeners;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.utils.SellChestItem;

public class SellChestListener implements Listener {

    private final DailyShopPlugin plugin;
    private final Economy economy;

    public SellChestListener(DailyShopPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRightClickChest(PlayerInteractEvent event) {
        // clic-droit main uniquement
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || !(clicked.getState() instanceof Container container)) return;

        Player player = event.getPlayer();
        ItemStack stick = player.getInventory().getItemInMainHand();
        if (!SellChestItem.isSellChestItem(stick)) return;

        // Vérifier le claim GriefPrevention
        Claim claim = GriefPrevention.instance.dataStore
                .getClaimAt(clicked.getLocation(), false, null);
        if (claim == null) {
            player.sendMessage("§cCe coffre n'est pas protégé – impossible de vendre ici.");
            return;
        }
        // allowContainers vérifie propriétaire OU container-trust
        String denialReason = claim.allowContainers(player);
        if (denialReason != null) {
            player.sendMessage("§cVous devez être propriétaire ou avoir la permission de container-trust ici.");
            return;
        }

        event.setCancelled(true);

        Shop shop = plugin.getShopManager().getCurrentShop();
        if (shop == null) {
            player.sendMessage("§cAucun shop actif pour vendre.");
            return;
        }

        double total = plugin.getSellManager().sellContainer(container.getInventory(), shop);
        if (total <= 0) {
            player.sendMessage("§eAucun item vendable dans ce coffre.");
            return;
        }

        economy.depositPlayer(player, total);
        player.sendMessage("§a+ " + total + " $ pour les items vendus dans ce coffre.");

        // gérer les utilisations du SellStick
        int uses = SellChestItem.getRemainingUses(stick);
        if (uses > 1) {
            SellChestItem.decreaseUses(stick);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
