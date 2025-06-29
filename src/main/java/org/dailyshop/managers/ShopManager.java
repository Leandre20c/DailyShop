package org.dailyshop.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.model.ShopItem;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ShopManager {

    private final DailyShopPlugin plugin;
    private final List<Shop> allShops = new ArrayList<>();

    private Shop currentShop;
    private Shop nextShop;

    public ShopManager(DailyShopPlugin plugin) {
        this.plugin = plugin;
        loadShops();
        restoreOrRotate();
    }

    /** Charge tous les shops depuis shops.yml */
    public void loadShops() {
        allShops.clear();  // 1) on vide l'ancienne liste

        File file = new File(plugin.getDataFolder(), "shops.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        var section = cfg.getConfigurationSection("shops");
        if (section == null) {
            plugin.getLogger().warning("Aucune section 'shops' dans shops.yml");
            return;
        }

        for (String key : section.getKeys(false)) {
            var sec = section.getConfigurationSection(key);
            if (sec == null) continue;

            String name = sec.getString("name", key);
            String skin = sec.getString("skin", "");
            List<ShopItem> items = new ArrayList<>();

            // 2) on boucle sur sec, pas sur section !
            for (Map<?,?> map : sec.getMapList("items")) {
                // 3a) récupération du price (obligatoire)
                Object rawPrice = map.get("price");
                if (rawPrice == null) {
                    plugin.getLogger().warning("Item sans price dans shop " + key);
                    continue;
                }
                double price = Double.parseDouble(rawPrice.toString());

                // 3b) récupération de la clé material OU custom-id
                String matKey = null;
                if (map.containsKey("material")) {
                    matKey = map.get("material").toString().trim();
                } else if (map.containsKey("custom-id")) {
                    matKey = map.get("custom-id").toString().trim();
                } else {
                    plugin.getLogger().warning("Item sans material/custom-id dans shop " + key);
                    continue;
                }

                // 3c) distinction vanilla vs custom
                Material mat = null;
                String customId = null;
                if (matKey.contains(":")) {
                    // itemsadder namespace:id
                    customId = matKey;
                } else {
                    try {
                        mat = Material.valueOf(matKey.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Matériel invalide (« " + matKey + " ») dans shop " + key);
                        continue;
                    }
                }
                items.add(new ShopItem(mat, customId, price));
            }

            allShops.add(new Shop(key, name, skin, items));
        }

        plugin.getLogger().info("Chargé " + allShops.size() + " shops depuis shops.yml.");
    }



    /** Restaure le shop du jour depuis la config ou lance une rotation */
    public void restoreOrRotate() {
        FileConfiguration cfg = plugin.getConfig();
        String lastDateStr = cfg.getString("last-rotation-date", "");
        LocalDate lastDate = lastDateStr.isBlank() ? null : LocalDate.parse(lastDateStr);
        LocalDate today    = LocalDate.now();

        // lit l’heure configurée (HH:mm) ou vide si désactivé
        String dailyTimeStr = cfg.getString("daily-reset-time", "").trim();

        if (!dailyTimeStr.isBlank()) {
            // 1) parse l’heure
            LocalTime resetTime = LocalTime.parse(dailyTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime nowTime   = LocalTime.now();

            // 2) même jour => on restaure
            if (lastDate != null && today.equals(lastDate)) {
                restoreFromConfig(cfg);
                return;
            }

            // 3) nouveau jour mais **avant** l’heure de reset => on restaure
            if (nowTime.isBefore(resetTime)) {
                restoreFromConfig(cfg);
                return;
            }

            // 4) nouveau jour **après** l’heure de reset => on tourne
            rotateShop();
            saveToConfig(today);
            return;
        }

        // --- fallback « reset-every-hours » ou rotation immédiate si daily-reset-time vide ---
        if (lastDate != null && today.equals(lastDate)) {
            restoreFromConfig(cfg);
        } else {
            rotateShop();
            saveToConfig(today);
        }
    }

    /** Restaure currentShop & nextShop depuis la config sans tourner */
    private void restoreFromConfig(FileConfiguration cfg) {
        String currentKey = cfg.getString("current-shop-key", "");
        String nextKey    = cfg.getString("next-shop-key", "");
        Shop c = findByKey(currentKey), n = findByKey(nextKey);
        if (c != null && n != null) {
            currentShop = c;
            nextShop    = n;
            plugin.getLogger().info("Shops restaurés hors-rotation : aujourd'hui="
                    + c.getName() + ", demain=" + n.getName());
            plugin.getNpcManager().applyCurrentShop();
        } else {
            // cas où on n'arrive pas à restaurer -> on tourne
            rotateShop();
            saveToConfig(LocalDate.now());
        }
    }


    /** Change le shop du jour et prépare celui de demain */
    public void rotateShop() {
        if (allShops.size() < 2) {
            plugin.getLogger().warning("Pas assez de shops pour rotater.");
            return;
        }
        if (currentShop == null) {
            Collections.shuffle(allShops);
            currentShop = allShops.get(0);
            nextShop    = allShops.get(1);
        } else {
            currentShop = nextShop;
            List<Shop> copy = new ArrayList<>(allShops);
            copy.remove(currentShop);
            Collections.shuffle(copy);
            nextShop = copy.get(0);
        }

        plugin.getLogger().info("Rotation : shop du jour="
                + currentShop.getName() + ", prochain=" + nextShop.getName());
        plugin.getNpcManager().applyCurrentShop();
    }

    private Shop findByKey(String key) {
        return allShops.stream()
                .filter(s -> s.getKey().equals(key))
                .findFirst().orElse(null);
    }

    /** Persiste la date et les clés de shop dans config.yml */
    private void saveToConfig(LocalDate date) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("last-rotation-date",   date.toString());
        cfg.set("current-shop-key",    currentShop.getKey());
        cfg.set("next-shop-key",       nextShop.getKey());
        plugin.saveConfig();
    }

    /** Exposé pour que la tâche Scheduled puisse persister l'état. */
    public void saveRotationState(LocalDate date) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("last-rotation-date", date.toString());
        cfg.set("current-shop-key", currentShop.getKey());
        cfg.set("next-shop-key", nextShop.getKey());
        plugin.saveConfig();
    }


    // ───── Accesseurs ─────

    public Shop getCurrentShop() { return currentShop; }
    public Shop getNextShop()    { return nextShop;    }
    public List<Shop> getAllShops() { return Collections.unmodifiableList(allShops); }
}
