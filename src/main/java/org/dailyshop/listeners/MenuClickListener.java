package org.dailyshop.listeners;

import dev.lone.itemsadder.api.ItemsAdder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.model.ShopItem;

import java.util.List;

public class MenuClickListener implements Listener {

    private final DailyShopPlugin plugin;
    private final Economy economy;

    public MenuClickListener(DailyShopPlugin plugin) {
        this.plugin = plugin;
        this.economy = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class)
                .getProvider();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getClickedInventory();
        if (inv == null) return;

        String title = ChatColor.stripColor(event.getView().getTitle());

        // Menu "Tous les marchands"
        if (title.equalsIgnoreCase("Tous les marchands")) {
            event.setCancelled(true);
            handleAllShopMenuClick(event, player);
            return;
        }

        // Menu d'un shop (du jour ou preview)
        boolean isShopMenu = plugin.getShopManager().getAllShops().stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(title));
        if (isShopMenu) {
            event.setCancelled(true);
            handleSellClick(event, player, title);
        }
    }

    private void handleAllShopMenuClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        String shopName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        Shop target = plugin.getShopManager().getAllShops().stream()
                .filter(s -> s.getName().equalsIgnoreCase(shopName))
                .findFirst().orElse(null);

        if (target != null) {
            plugin.getMenuManager().openShopMenu(player, target);
        }
    }

    private void handleSellClick(InventoryClickEvent event, Player player, String shopName) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Shop shop = plugin.getShopManager().getAllShops().stream()
                .filter(s -> s.getName().equalsIgnoreCase(shopName))
                .findFirst().orElse(null);
        if (shop == null) return;

        // Trouve le ShopItem (vanilla ou custom)
        ShopItem target = shop.getItems().stream()
                .filter(it -> it.matches(clicked))
                .findFirst().orElse(null);
        if (target == null) return;

        // Empêche la vente en aperçu
        if (plugin.getShopManager().getNextShop().getName()
                .equalsIgnoreCase(shop.getName())) {
            player.sendMessage("§eCeci est un aperçu. Vous ne pouvez pas vendre ici.");
            return;
        }

        int sold;
        ClickType click = event.getClick();
        if (click == ClickType.LEFT) {
            sold = removeFromInventory(player, target, 1);
        } else if (click == ClickType.SHIFT_LEFT) {
            sold = removeAllFromInventory(player, target);
        } else if (click == ClickType.CONTROL_DROP) {
            sold = removeFromInventory(player, target, 64);
        } else {
            return;
        }

        if (sold <= 0) {
            player.sendMessage("§cVous n'avez pas d'objets à vendre.");
            return;
        }

        double total = target.getPrice() * sold;
        economy.depositPlayer(player, total);
        String itemName = target.isCustom()
                ? target.getCustomId().split(":", 2)[1]
                : target.getMaterial().name().toLowerCase().replace("_", " ");
        player.sendMessage("§a+ " + total + "$ pour " + sold + "× " + itemName);

        player.playSound(player.getLocation(),
                "minecraft:entity.player.levelup",
                1f, 1.5f);
        player.spawnParticle(Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1, 0),
                10, 0.3, 0.5, 0.3);
    }

    /** Vend jusqu'à max unités de `target` trouvées dans l'inventaire */
    private int removeFromInventory(Player player, ShopItem target, int max) {
        int remaining = max;
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && target.matches(item)) {
                int amt = item.getAmount();
                if (amt > remaining) {
                    item.setAmount(amt - remaining);
                    removed += remaining;
                    remaining = 0;
                } else {
                    removed += amt;
                    remaining -= amt;
                    contents[i] = null;
                }
            }
        }

        player.getInventory().setContents(contents);
        return removed;
    }

    /** Vend tous les exemplaires de `target` dans l'inventaire */
    private int removeAllFromInventory(Player player, ShopItem target) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && target.matches(item)) {
                removed += item.getAmount();
                contents[i] = null;
            }
        }

        player.getInventory().setContents(contents);
        return removed;
    }
}
