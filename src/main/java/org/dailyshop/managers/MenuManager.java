// src/main/java/org/dailyshop/managers/MenuManager.java
package org.dailyshop.managers;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.model.ShopItem;
import org.dailyshop.utils.ItemGroups;

import java.util.*;

/**
 * MenuManager affiche le menu et anime certains blocs à variantes
 * (laine, froglight…) en faisant tourner les matériaux toutes les 20 ticks (1s).
 */
public class MenuManager {

    private final DailyShopPlugin plugin;
    private final Map<Inventory, BukkitTask> cycleTasks = new HashMap<>();

    public MenuManager(DailyShopPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ouvre le menu pour un shop donné, en mode normal ou preview.
     */
    public void openShopMenu(Player player, Shop shop, boolean preview) {
        // annule l'ancien cycle
        Inventory old = player.getOpenInventory().getTopInventory();
        if (old != null && cycleTasks.containsKey(old)) {
            cycleTasks.remove(old).cancel();
        }

        List<ShopItem> items = shop.getItems();
        int size = Math.min(((items.size() / 9) + 1) * 9, 54);
        String title = "§b" + shop.getName() + (preview ? " (Preview)" : "");
        Inventory inv = Bukkit.createInventory(null, size, title);

        // centre horizontalement
        int startSlot = (size - items.size()) / 2;
        Map<Integer, ShopItem> slotToItem = new HashMap<>();
        Map<Integer, Integer> slotToVariant = new HashMap<>();

        int slot = startSlot;
        for (ShopItem si : items) {
            slotToItem.put(slot, si);
            slotToVariant.put(slot, 0);
            inv.setItem(slot, buildStack(si, preview));
            slot++;
        }

        player.openInventory(inv);

        // animation pour vanilla
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (var e : slotToItem.entrySet()) {
                int s = e.getKey();
                ShopItem si = e.getValue();
                if (si.isCustom()) continue;
                List<Material> group = ItemGroups.VARIANTS.get(si.getMaterial());
                if (group == null) continue;
                int idx = (slotToVariant.get(s) + 1) % group.size();
                slotToVariant.put(s, idx);
                ItemStack newStack = new ItemStack(group.get(idx));
                ItemMeta m = newStack.getItemMeta();
                if (m != null) {
                    ItemStack old1 = inv.getItem(s);
                    ItemMeta oldM = old1.getItemMeta();
                    m.setDisplayName(oldM.getDisplayName());
                    m.setLore(oldM.getLore());
                    newStack.setItemMeta(m);
                }
                inv.setItem(s, newStack);
            }
        }, 20L, 20L);

        cycleTasks.put(inv, task);
    }

    /**
     * Construit l'ItemStack initial pour un ShopItem (vanilla ou custom).
     */
    private ItemStack buildStack(ShopItem si, boolean preview) {
        ItemStack item;
        if (si.isCustom()) {
            CustomStack cs = CustomStack.getInstance(si.getCustomId());
            item = cs != null ? cs.getItemStack() : new ItemStack(Material.BARRIER);
        } else {
            item = new ItemStack(si.getMaterial());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = si.isCustom()
                    ? si.getCustomId().split(":",2)[1]
                    : formatName(si.getMaterial());
            meta.setDisplayName("§f" + name);

            List<String> lore = new ArrayList<>();
            lore.add("§7Prix: §a" + si.getPrice() + " PE");
            if (preview) lore.add("§c(Non vendable dans l'aperçu)");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatName(Material m) {
        String s = m.name().toLowerCase().replace("_"," ");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Affiche le menu listant tous les shops disponibles.
     */
    public void openAllShopsMenu(Player player) {
        List<Shop> shops = plugin.getShopManager().getAllShops();
        int size = ((shops.size() / 9) + 1) * 9;
        Inventory inv = Bukkit.createInventory(null, size, "§eTous les marchands");

        for (Shop shop : shops) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + shop.getName());

                List<String> lore = new ArrayList<>();
                lore.add("§7Clique pour voir les articles !");
                lore.add(""); // ligne vide pour séparer
                lore.add("§eArticles disponibles:");

                // Ajoute chaque item avec son prix
                for (ShopItem item : shop.getItems()) {
                    String itemName;
                    if (item.isCustom()) {
                        itemName = item.getCustomId().split(":", 2)[1];
                    } else {
                        itemName = formatName(item.getMaterial());
                    }
                    lore.add("§f• " + itemName + " §7- §a" + item.getPrice() + " PE");
                }

                meta.setLore(lore);
                OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.getSkin());
                meta.setOwningPlayer(owner);
                skull.setItemMeta(meta);
            }
            inv.addItem(skull);
        }

        player.openInventory(inv);
    }

    /**
     * Affiche le menu paginé de tous les items disponibles dans tous les shops.
     */
    public void openAllItemsMenu(Player player, int page) {
        // Collecte tous les items de tous les shops
        Set<ShopItem> uniqueItems = new HashSet<>();
        for (Shop shop : plugin.getShopManager().getAllShops()) {
            uniqueItems.addAll(shop.getItems());
        }

        List<ShopItem> allItems = new ArrayList<>(uniqueItems);
        allItems.sort((a, b) -> {
            String nameA = a.isCustom() ? a.getCustomId() : a.getMaterial().name();
            String nameB = b.isCustom() ? b.getCustomId() : b.getMaterial().name();
            return nameA.compareToIgnoreCase(nameB);
        });

        // Configuration pagination
        int itemsPerPage = 45; // 5 lignes de 9 items (garde 1 ligne pour navigation)
        int totalPages = (int) Math.ceil((double) allItems.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, totalPages));

        String title = String.format("§eArticles (Page %d/%d)", page, totalPages);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Calcule les indices pour cette page
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());

        // Ajoute les items de cette page
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = allItems.get(i);
            inv.setItem(slot, buildItemStackForAllItems(item));
            slot++;
        }

        // Boutons de navigation (ligne du bas)
        if (page > 1) {
            // Bouton page précédente
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§a◀ Page précédente");
                prevMeta.setLore(List.of("§7Page " + (page - 1) + "/" + totalPages));
                prevButton.setItemMeta(prevMeta);
            }
            inv.setItem(48, prevButton);
        }

        if (page < totalPages) {
            // Bouton page suivante
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§aPage suivante ▶");
                nextMeta.setLore(List.of("§7Page " + (page + 1) + "/" + totalPages));
                nextButton.setItemMeta(nextMeta);
            }
            inv.setItem(50, nextButton);
        }

        // Bouton retour menu principal
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cRetour au menu principal");
            backMeta.setLore(List.of("§7Cliquez pour revenir"));
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(49, backButton);

        // Info page actuelle
        ItemStack infoButton = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoButton.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§ePage " + page + "/" + totalPages);
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Total d'articles: §e" + allItems.size());
            infoLore.add("§7Articles sur cette page: §e" + (endIndex - startIndex));
            infoMeta.setLore(infoLore);
            infoButton.setItemMeta(infoMeta);
        }
        inv.setItem(45, infoButton);

        player.openInventory(inv);
    }

    /**
     * Construit l'ItemStack pour le menu de tous les items avec des infos supplémentaires.
     */
    private ItemStack buildItemStackForAllItems(ShopItem si) {
        ItemStack item;
        if (si.isCustom()) {
            CustomStack cs = CustomStack.getInstance(si.getCustomId());
            item = cs != null ? cs.getItemStack().clone() : new ItemStack(Material.BARRIER);
        } else {
            item = new ItemStack(si.getMaterial());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = si.isCustom()
                    ? si.getCustomId().split(":", 2)[1]
                    : formatName(si.getMaterial());
            meta.setDisplayName("§f" + name);

            List<String> lore = new ArrayList<>();
            lore.add("§7Prix: §a" + si.getPrice() + " PE");

            // Trouve dans quels shops cet item est disponible
            List<String> shopNames = new ArrayList<>();
            for (Shop shop : plugin.getShopManager().getAllShops()) {
                if (shop.getItems().contains(si)) {
                    shopNames.add(shop.getName());
                }
            }

            if (!shopNames.isEmpty()) {
                lore.add("§7Disponible chez:");
                for (String shopName : shopNames) {
                    lore.add("§e• " + shopName);
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}