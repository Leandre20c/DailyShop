package org.dailyshop.anim;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.dailyshop.managers.MenuManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryAnimator {

    private static final Map<Inventory, List<AnimatedSlot>> animatedInventories = new ConcurrentHashMap<>();

    /** Appelé depuis MenuManager après avoir ouvert un shop. */
    public static void register(Inventory inv, List<AnimatedSlot> slots) {
        animatedInventories.put(inv, slots);
    }

    /** Lance le scheduler, à appeler dans onEnable(). */
    public static void start(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (var entry : animatedInventories.entrySet()) {
                Inventory inv = entry.getKey();
                // Si l'inventaire est fermé ou invalide, on le retire
                if (!inv.getViewers().isEmpty()) {
                    for (AnimatedSlot slot : entry.getValue()) {
                        slot.advance();
                        inv.setItem(slot.getSlotIndex(), slot.buildStack());
                    }
                } else {
                    animatedInventories.remove(inv);
                }
            }
        }, 20L, 20L);
    }
}
