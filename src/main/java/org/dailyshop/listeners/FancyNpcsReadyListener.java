package org.dailyshop.listeners;

import de.oliver.fancynpcs.api.events.NpcsLoadedEvent;  // ← corriger ici
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.dailyshop.DailyShopPlugin;

public class FancyNpcsReadyListener implements Listener {

    private final DailyShopPlugin plugin;

    public FancyNpcsReadyListener(DailyShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcsLoaded(NpcsLoadedEvent event) {
        if (plugin.getNpcManager().loadNPC()) {
            plugin.getLogger().info("✅ NPC Manager initialisé après le chargement de FancyNPCs.");
            plugin.getNpcManager().applyCurrentShop();
        } else {
            plugin.getLogger().severe("❌ Impossible de charger le NPC FancyNPCs !");
        }
    }
}
