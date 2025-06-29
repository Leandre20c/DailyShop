package org.dailyshop.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.utils.TomorrowShopItem;

public class TomorrowItemListener implements Listener {

    public TomorrowItemListener(DailyShopPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!TomorrowShopItem.isTomorrowShopItem(item)) return;

        event.setCancelled(true);

        Shop next = DailyShopPlugin.getInstance().getShopManager().getNextShop();
        if (next == null) {
            player.sendMessage("§cAucun shop prévu pour demain.");
            return;
        }

        DailyShopPlugin.getInstance().getMenuManager().openShopMenu(player, next);

        // ✅ Consomme l’item
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
    }
}
