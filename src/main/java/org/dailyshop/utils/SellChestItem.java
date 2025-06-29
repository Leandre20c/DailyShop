package org.dailyshop.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.dailyshop.DailyShopPlugin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SellChestItem {

    private static final NamespacedKey KEY = new NamespacedKey(DailyShopPlugin.getInstance(), "sellstick");
    private static final Pattern USES_PATTERN = Pattern.compile("§8Utilisations restantes : §e(\\d+)");

    public static ItemStack getItem(int uses) {
        ItemStack item = new ItemStack(Material.BREEZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§3§lSellStick");
            meta.setLore(List.of(
                    "§7Clic-droit sur un coffre pour vendre les items.",
                    "§8Utilisations restantes : §e" + uses
            ));
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, uses);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isSellChestItem(ItemStack item) {
        if (item == null || item.getType() != Material.BREEZE_ROD) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(KEY, PersistentDataType.INTEGER);
    }

    public static int getRemainingUses(ItemStack item) {
        if (!isSellChestItem(item)) return 0;

        ItemMeta meta = item.getItemMeta();
        Integer stored = meta.getPersistentDataContainer().get(KEY, PersistentDataType.INTEGER);
        if (stored != null) return stored;

        // fallback (parsing du lore si jamais)
        if (meta.getLore() != null) {
            for (String line : meta.getLore()) {
                Matcher matcher = USES_PATTERN.matcher(line);
                if (matcher.find()) {
                    try {
                        return Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return 0;
    }

    public static boolean decreaseUses(ItemStack item) {
        if (!isSellChestItem(item)) return false;

        int current = getRemainingUses(item);
        if (current <= 1) return false;

        int newValue = current - 1;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        meta.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, newValue);

        // met à jour le lore aussi
        List<String> lore = meta.getLore();
        if (lore != null && lore.size() > 1) {
            lore.set(1, "§8Utilisations restantes : §e" + newValue);
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return true;
    }
}
