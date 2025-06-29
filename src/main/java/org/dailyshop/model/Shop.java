package org.dailyshop.model;

import org.bukkit.Material;

import java.util.List;

public class Shop {
    private final String key;       // clé YAML, ex. "shop1"
    private final String name;      // affiché au joueur
    private final String skin;      // nom du skin
    private final List<ShopItem> items;

    public Shop(String key, String name, String skin, List<ShopItem> items) {
        this.key   = key;
        this.name  = name;
        this.skin  = skin;
        this.items = items;
    }

    public String getKey()  { return key;  }
    public String getName() { return name; }
    public String getSkin() { return skin; }
    public List<ShopItem> getItems() { return items; }
}
