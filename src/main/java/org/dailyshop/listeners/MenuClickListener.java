// src/main/java/org/dailyshop/listeners/MenuClickListener.java
package org.dailyshop.listeners;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.model.ShopItem;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuClickListener implements Listener {

    private final DailyShopPlugin plugin;
    private final Economy economy;

    public MenuClickListener(DailyShopPlugin plugin) {
        this.plugin = plugin;
        this.economy = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class)
                .getProvider();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent ev) {
        if (!(ev.getWhoClicked() instanceof Player p)) return;

        String rawTitle = ev.getView().getTitle();
        String strippedTitle = ChatColor.stripColor(rawTitle);

        // Menu "Tous les marchands"
        if (strippedTitle.equalsIgnoreCase("Tous les marchands")) {
            ev.setCancelled(true);
            handleAllShopsMenuClick(p, ev);
            return;
        }

        // Menu paginé des articles
        if (strippedTitle.startsWith("Articles (Page ")) {
            ev.setCancelled(true);
            handleAllItemsMenuClick(p, ev, strippedTitle);
            return;
        }

        // Gestion des shops individuels (code existant)
        boolean preview = rawTitle.endsWith("(Preview)");
        String title = ChatColor.stripColor(
                preview
                        ? rawTitle.substring(0, rawTitle.length() - "(Preview)".length()).trim()
                        : rawTitle
        );

        // Recherche du shop par nom
        Shop shop = plugin.getShopManager().getAllShops().stream()
                .filter(s -> s.getName().equalsIgnoreCase(title))
                .findFirst().orElse(null);
        if (shop == null) return;

        ev.setCancelled(true);

        // Interdire toute action en mode preview
        if (preview) {
            p.sendMessage("§eCeci est un aperçu. Vous ne pouvez pas vendre ici.");
            return;
        }

        ItemStack clicked = ev.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Trouve le ShopItem (vanilla + variantes ou custom)
        ShopItem target = shop.getItems().stream()
                .filter(it -> it.matches(clicked))
                .findFirst().orElse(null);
        if (target == null) return;

        int sold;
        ClickType ct = ev.getClick();
        if (ct == ClickType.LEFT) {
            sold = removeFromInventory(p, target, 1);
        } else if (ct == ClickType.SHIFT_LEFT) {
            sold = removeAllFromInventory(p, target);
        } else if (ct == ClickType.CONTROL_DROP) {
            sold = removeFromInventory(p, target, 64);
        } else {
            return;
        }

        if (sold <= 0) {
            p.sendMessage("§cVous n'avez pas d'objets à vendre.");
            return;
        }

        double total = target.getPrice() * sold;
        plugin.getSellManager().payAndRecord(p, Map.of(target, sold));

        String name = target.isCustom()
                ? target.getCustomId().split(":",2)[1]
                : target.getMaterial().name().toLowerCase().replace("_"," ");
        p.sendMessage("§a+ " + total + " PE pour " + sold + "× " + name);

        p.playSound(p.getLocation(), "entity.player.levelup", 1f, 1.5f);
        p.spawnParticle(
                Particle.HAPPY_VILLAGER,
                p.getLocation().add(0,1,0),
                10, 0.3, 0.5, 0.3
        );
    }

    /**
     * Gère les clics dans le menu "Tous les marchands"
     */
    private void handleAllShopsMenuClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        ItemMeta meta = clicked.getItemMeta();
        if (!(meta instanceof SkullMeta)) return;

        String shopName = ChatColor.stripColor(meta.getDisplayName());
        Shop shop = plugin.getShopManager().getAllShops().stream()
                .filter(s -> s.getName().equalsIgnoreCase(shopName))
                .findFirst().orElse(null);

        if (shop != null) {
            plugin.getMenuManager().openShopMenu(player, shop, true);
        }
    }

    /**
     * Gère les clics dans le menu paginé des articles
     */
    private void handleAllItemsMenuClick(Player player, InventoryClickEvent event, String title) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Extraction du numéro de page actuel depuis le titre
        Pattern pattern = Pattern.compile("Articles \\(Page (\\d+)/(\\d+)\\)");
        Matcher matcher = pattern.matcher(title);
        if (!matcher.find()) return;

        int currentPage = Integer.parseInt(matcher.group(1));
        int totalPages = Integer.parseInt(matcher.group(2));

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String displayName = ChatColor.stripColor(meta.getDisplayName());

        // Navigation entre les pages
        if (clicked.getType() == Material.ARROW) {
            if (displayName.contains("Page précédente") && currentPage > 1) {
                plugin.getMenuManager().openAllItemsMenu(player, currentPage - 1);
            } else if (displayName.contains("Page suivante") && currentPage < totalPages) {
                plugin.getMenuManager().openAllItemsMenu(player, currentPage + 1);
            }
        }
        // Bouton retour
        else if (clicked.getType() == Material.BARRIER && displayName.contains("Retour au menu principal")) {
            plugin.getMenuManager().openAllShopsMenu(player);
        }
        // Clic sur un item - afficher les shops qui le vendent
        else if (event.getSlot() < 45) { // Seulement les slots d'items (pas les boutons de navigation)
            handleItemClick(player, clicked);
        }
    }

    /**
     * Gère le clic sur un item dans le menu de tous les items
     */
    private void handleItemClick(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return;

        // Trouve le premier shop qui vend cet item (depuis le lore)
        for (String loreLine : meta.getLore()) {
            String stripped = ChatColor.stripColor(loreLine);
            if (stripped.startsWith("• ")) {
                String shopName = stripped.substring(2); // Enlève "• "
                Shop shop = plugin.getShopManager().getAllShops().stream()
                        .filter(s -> s.getName().equalsIgnoreCase(shopName))
                        .findFirst().orElse(null);

                if (shop != null) {
                    plugin.getMenuManager().openShopMenu(player, shop, false);
                    return;
                }
            }
        }
    }

    private int removeFromInventory(Player p, ShopItem target, int max) {
        int rem = max, sold = 0;
        ItemStack[] cont = p.getInventory().getContents();
        for (int i = 0; i < cont.length && rem > 0; i++) {
            ItemStack it = cont[i];
            if (it != null && target.matches(it)) {
                int amt = it.getAmount();
                if (amt > rem) {
                    it.setAmount(amt - rem);
                    sold += rem;
                    rem = 0;
                } else {
                    sold += amt;
                    rem -= amt;
                    cont[i] = null;
                }
            }
        }
        p.getInventory().setContents(cont);
        return sold;
    }

    private int removeAllFromInventory(Player p, ShopItem target) {
        int sold = 0;
        ItemStack[] cont = p.getInventory().getContents();
        for (int i = 0; i < cont.length; i++) {
            ItemStack it = cont[i];
            if (it != null && target.matches(it)) {
                sold += it.getAmount();
                cont[i] = null;
            }
        }
        p.getInventory().setContents(cont);
        return sold;
    }
}