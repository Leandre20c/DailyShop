package org.dailyshop.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;
import org.dailyshop.model.ShopItem;

import java.util.HashMap;
import java.util.Map;

public class SellCommand implements CommandExecutor {

    private final DailyShopPlugin plugin;
    private final Economy economy;

    public SellCommand(DailyShopPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eUtilisation : /sell <hand|handall|all>");
            return true;
        }

        Shop shop = plugin.getShopManager().getCurrentShop();
        if (shop == null) {
            player.sendMessage("§cAucun shop chargé.");
            return true;
        }

        Map<ShopItem, Integer> sold = new HashMap<>();
        ItemStack[] contents = player.getInventory().getContents();

        switch (args[0].toLowerCase()) {
            case "hand" -> {
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()) {
                    player.sendMessage("§cVous ne tenez rien.");
                    return true;
                }
                for (ShopItem shopItem : shop.getItems()) {
                    if (shopItem.matches(inHand)) {
                        sold.put(shopItem, 1);
                        inHand.setAmount(inHand.getAmount() - 1);
                        break;
                    }
                }
            }

            case "handall" -> {
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()) {
                    player.sendMessage("§cVous ne tenez rien.");
                    return true;
                }
                for (ShopItem shopItem : shop.getItems()) {
                    if (shopItem.matches(inHand)) {
                        int total = 0;
                        for (int i = 0; i < contents.length; i++) {
                            ItemStack item = contents[i];
                            if (shopItem.matches(item)) {
                                total += item.getAmount();
                                contents[i] = null;
                            }
                        }
                        if (total > 0) {
                            sold.put(shopItem, total);
                        }
                        break;
                    }
                }
                player.getInventory().setContents(contents);
            }

            case "all" -> {
                for (ShopItem shopItem : shop.getItems()) {
                    int total = 0;
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack item = contents[i];
                        if (shopItem.matches(item)) {
                            total += item.getAmount();
                            contents[i] = null;
                        }
                    }
                    if (total > 0) {
                        sold.put(shopItem, sold.getOrDefault(shopItem, 0) + total);
                    }
                }
                player.getInventory().setContents(contents);
            }

            default -> {
                player.sendMessage("§cUsage: /sell <hand|handall|all>");
                return true;
            }
        }

        double totalMoney = plugin.getSellManager().calculateTotal(sold);
        if (totalMoney > 0) {
            economy.depositPlayer(player, totalMoney);
            player.sendMessage("§aVous avez vendu pour §e" + totalMoney + " $");
        } else {
            player.sendMessage("§cAucun item vendable trouvé.");
        }

        return true;
    }
}
