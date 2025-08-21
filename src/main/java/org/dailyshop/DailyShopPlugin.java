package org.dailyshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.dailyshop.commands.*;
import org.dailyshop.listeners.FancyNpcsReadyListener;
import org.dailyshop.listeners.MenuClickListener;
import org.dailyshop.listeners.SellChestListener;
import org.dailyshop.listeners.TomorrowItemListener;
import org.dailyshop.managers.NPCManager;
import org.dailyshop.managers.ShopManager;
import org.dailyshop.managers.MenuManager;
import org.dailyshop.managers.SellManager;
import org.dailyshop.statistics.StatsManager;
import org.dailyshop.tasks.ShopRotationTask;

import java.io.File;
import java.util.List;

public final class DailyShopPlugin extends JavaPlugin {

    private static DailyShopPlugin instance;

    private ShopManager shopManager;
    private NPCManager npcManager;
    private MenuManager menuManager;
    private SellManager sellManager;
    private Economy economy;
    private StatsManager statsManager;

    public static DailyShopPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault non trouvé ! Plugin désactivé.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        File exportFolder = new File(getDataFolder(), "exports");
        if (!exportFolder.exists()) exportFolder.mkdirs();

        instance = this;

        saveDefaultConfig();
        loadShopsFile();

        // ─────── Initialisation des managers ───────
        this.npcManager  = new NPCManager(this);  // **d'abord** NPCManager
        this.shopManager = new ShopManager(this); // puis ShopManager
        this.menuManager = new MenuManager(this);
        this.sellManager = new SellManager(this, getEconomy());
        this.statsManager = new StatsManager(exportFolder);

        // ─────── Configuration des aliases ───────
        initAliases();

        // ─────── Listener pour CitizensReady ───────
        Bukkit.getPluginManager().registerEvents(new FancyNpcsReadyListener(this), this);

        // ─────── Enregistrement des commandes ───────
        registerCommands();

        // ─────── Tasks & Listeners ───────
        initTasksAndListeners();

        getLogger().info("DailyShop enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DailyShop disabled.");
    }

    private void initAliases() {
        String openAlias = getConfig().getString("aliases.open", "shop");
        String sellAlias = getConfig().getString("aliases.sell", "vendre");

        List<String> openAliasOptions = List.of("shop", "boutique", "market", "dshop");
        List<String> sellAliasOptions = List.of("vendre", "revendre", "vente");

        if (openAliasOptions.contains(openAlias) && getCommand(openAlias) != null) {
            getCommand(openAlias).setExecutor(new AliasCommand(new DailyShopCommand(this), "open"));
            getCommand(openAlias).setTabCompleter(new AliasTabCompleter(
                    new DailyShopTabCompleter(), "open"));
            getLogger().info("Alias pour /dailyshop open enregistré sous /" + openAlias);
        } else {
            getLogger().warning("Alias open '" + openAlias + "' non valide ou non déclaré dans plugin.yml.");
        }

        if (sellAliasOptions.contains(sellAlias) && getCommand(sellAlias) != null) {
            getCommand(sellAlias).setExecutor(new AliasCommand(new SellCommand(this, economy)));
            getCommand(sellAlias).setTabCompleter(new SellTabCompleter());
            getLogger().info("Alias pour /sell enregistré sous /" + sellAlias);
        } else {
            getLogger().warning("Alias sell '" + sellAlias + "' non valide ou non déclaré dans plugin.yml.");
        }
    }

    private void registerCommands() {
        // principale
        getCommand("dailyshop").setExecutor(new DailyShopCommand(this));
        getCommand("dailyshop").setTabCompleter(new DailyShopTabCompleter());
        // sell
        getCommand("sell").setExecutor(new SellCommand(this, economy));
        getCommand("sell").setTabCompleter(new SellTabCompleter());
    }

    private void initTasksAndListeners() {
        new ShopRotationTask(this).schedule();
        new MenuClickListener(this);
        new TomorrowItemListener(this);
        new SellChestListener(this);
    }

    private void loadShopsFile() {
        File shopsFile = new File(getDataFolder(), "shops.yml");
        if (!shopsFile.exists()) {
            saveResource("shops.yml", false);
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        var rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        this.economy = rsp.getProvider();
        return this.economy != null;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
    public NPCManager getNpcManager() {
        return npcManager;
    }
    public MenuManager getMenuManager() {
        return menuManager;
    }
    public SellManager getSellManager() {
        return sellManager;
    }
    public Economy getEconomy() {
        return economy;
    }
    public StatsManager getStatsManager() {return statsManager;}
}
