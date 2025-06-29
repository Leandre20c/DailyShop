package org.dailyshop.managers;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.model.ShopItem;

import java.util.*;

/**
 * MenuManager affiche le menu et anime certains blocs à variantes (laine, froglight…)
 * en faisant tourner les matériaux toutes les 20 ticks (1 seconde).
 */
public class MenuManager {

    private final DailyShopPlugin plugin;

    /**
     * Pour chaque inventaire ouvert, on garde la tâche de cycle afin de pouvoir l'annuler si besoin
     */
    private final Map<Inventory, BukkitTask> cycleTasks = new HashMap<>();

    /**
     * Définissez ici vos groupes de variantes.
     */
    private static final Map<Material, List<Material>> VARIANTS = Map.of(
            // laine : cycle white → red → blue → green → …
            Material.WHITE_WOOL, List.of(
                    Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
                    Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
                    Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
                    Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
                    Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL,
                    Material.BLACK_WOOL
            ),
            // froglight
            Material.OCHRE_FROGLIGHT, List.of(
                    Material.OCHRE_FROGLIGHT, Material.VERDANT_FROGLIGHT,
                    Material.PEARLESCENT_FROGLIGHT
            ),

            // eggs
            Material.EGG, List.of(
                    Material.EGG, Material.BROWN_EGG, Material.BLUE_EGG
            )

            // ajoutez d'autres groupes si nécessaire
    );

    public MenuManager(DailyShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void openShopMenu(Player player, Shop shop) {
        // Si un cycle était déjà en cours sur cet inventaire, on l'annule
        Inventory old = player.getOpenInventory().getTopInventory();
        if (old != null && cycleTasks.containsKey(old)) {
            cycleTasks.remove(old).cancel();
        }

        List<ShopItem> items = shop.getItems();
        boolean isPreview = plugin.getShopManager().getNextShop().getKey()
                .equals(shop.getKey());

        int size = ((items.size() / 9) + 1) * 9;
        size = Math.min(size, 54);
        Inventory inv = Bukkit.createInventory(null, size, "§b" + shop.getName());

        // On mémorise pour chaque slot son ShopItem et sa variante
        Map<Integer, ShopItem> slotToItem = new HashMap<>();
        Map<Integer, Integer> slotToVariantIndex = new HashMap<>();

        int slot = 0;
        for (ShopItem shopItem : items) {
            // on trouve le prochain slot libre (skip si occupé)
            while (inv.getItem(slot) != null) slot++;
            slotToItem.put(slot, shopItem);
            slotToVariantIndex.put(slot, 0);

            // pose l'item initial
            inv.setItem(slot, buildStack(shopItem, isPreview));
            slot++;
        }

        player.openInventory(inv);

        // Lance l'animation si nécessaire
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (int s : slotToItem.keySet()) {
                ShopItem si = slotToItem.get(s);
                Material base = si.isCustom() ? null : si.getMaterial();
                List<Material> group = VARIANTS.get(base);

                if (group != null) {
                    // cycle sur les variantes
                    int idx = slotToVariantIndex.get(s);
                    idx = (idx + 1) % group.size();
                    slotToVariantIndex.put(s, idx);

                    Material mat = group.get(idx);
                    ItemStack newStack = new ItemStack(mat);
                    ItemMeta m = newStack.getItemMeta();
                    if (m != null) {
                        m.setDisplayName(inv.getItem(s).getItemMeta().getDisplayName());
                        m.setLore(inv.getItem(s).getItemMeta().getLore());
                        newStack.setItemMeta(m);
                    }
                    inv.setItem(s, newStack);
                }
            }
        }, 20L, 20L);

        cycleTasks.put(inv, task);
    }

    /**
     * Construit l'ItemStack initial pour un ShopItem (vanilla ou custom).
     */
    private ItemStack buildStack(ShopItem shopItem, boolean isPreview) {
        ItemStack item;
        if (shopItem.isCustom()) {
            CustomStack cs = CustomStack.getInstance(shopItem.getCustomId());
            item = (cs != null ? cs.getItemStack() : new ItemStack(Material.BARRIER));
        } else {
            item = new ItemStack(shopItem.getMaterial());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String display = shopItem.isCustom()
                    ? shopItem.getCustomId().split(":", 2)[1]
                    : formatMaterialName(shopItem.getMaterial());
            meta.setDisplayName("§f" + display);

            List<String> lore = new ArrayList<>();
            lore.add("§7Prix: §a" + shopItem.getPrice() + " $");
            if (isPreview) {
                lore.add("§c(Non vendable dans l'aperçu)");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public void openAllShopsMenu(Player player) {
        List<Shop> shops = plugin.getShopManager().getAllShops();
        int size = ((shops.size() / 9) + 1) * 9;
        Inventory inv = Bukkit.createInventory(null, size, "§eTous les marchands");

        for (Shop shop : shops) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = skull.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + shop.getName());
                List<String> lore = new ArrayList<>();
                lore.add("§7Clique pour voir les articles !");
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            skull = Bukkit.getUnsafe().modifyItemStack(skull, "{SkullOwner:\"" + shop.getSkin() + "\"}");
            inv.addItem(skull);
        }

        player.openInventory(inv);
    }
}
