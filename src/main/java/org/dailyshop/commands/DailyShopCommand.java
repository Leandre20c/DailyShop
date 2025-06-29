package org.dailyshop.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;

public class DailyShopCommand implements CommandExecutor {

    private final DailyShopPlugin plugin;

    public DailyShopCommand(DailyShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/dailyshop <open|menu|giveitem|reload|rotate>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "open" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande réservée aux joueurs.");
                    return true;
                }
                Shop shop = plugin.getShopManager().getCurrentShop();
                if (shop == null) {
                    player.sendMessage("§cLe shop du jour n’est pas disponible.");
                    return true;
                }
                plugin.getMenuManager().openShopMenu(player, shop);
            }

            case "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande réservée aux joueurs.");
                    return true;
                }
                plugin.getMenuManager().openAllShopsMenu(player);
            }

            case "giveitem" -> {
                if (!sender.hasPermission("dailyshop.admin")) {
                    sender.sendMessage("§cPermission refusée.");
                    return true;
                }
                // on détermine si le 1er arg est un joueur en ligne
                Player target;
                String typeArg;
                int idx = 1;
                if (args.length > 1 && Bukkit.getPlayerExact(args[1]) != null) {
                    target = Bukkit.getPlayerExact(args[1]);
                    idx = 2;  // le type (sell / preview) se trouve en args[2]
                } else {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage("§cTu dois spécifier un joueur ou être un joueur toi-même.");
                        return true;
                    }
                    target = p;
                }

                // on récupère le type d'item (sell ou preview)
                if (args.length <= idx) {
                    sender.sendMessage("§cUsage: /dailyshop giveitem [joueur] <sell|preview> [uses]");
                    return true;
                }
                typeArg = args[idx].toLowerCase();

                if (typeArg.equals("sell")) {
                    // nombre d'utilisations (optionnel)
                    int uses = plugin.getConfig().getInt("items.sell-stick.default-uses", 10);
                    if (args.length > idx+1) {
                        try {
                            uses = Math.max(1, Integer.parseInt(args[idx+1]));
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cLe nombre d'utilisations doit être un entier valide.");
                            return true;
                        }
                    }
                    target.getInventory().addItem(org.dailyshop.utils.SellChestItem.getItem(uses));
                    sender.sendMessage("§aSellStick donné à §e" + target.getName()
                            + " §aavec §e" + uses + " §autilisations.");
                    if (!target.equals(sender)) {
                        target.sendMessage("§aTu as reçu un SellStick avec §e" + uses + " §autilisations !");
                    }
                } else if (typeArg.equals("preview")) {
                    target.getInventory().addItem(org.dailyshop.utils.TomorrowShopItem.getItem());
                    sender.sendMessage("§aItem de prévisualisation donné à §e" + target.getName() + "§a.");
                    if (!target.equals(sender)) {
                        target.sendMessage("§aTu as reçu un œil de cristal pour voir le shop de demain !");
                    }
                } else {
                    sender.sendMessage("§cType invalide, utilise « sell » ou « preview ».");
                }
            }



            case "reload" -> {
                if (!sender.hasPermission("dailyshop.admin")) {
                    sender.sendMessage("§cPermission refusée.");
                    return true;
                }
                plugin.reloadConfig();                      // 1) recharger plugin.yml + config.yml
                plugin.getShopManager().loadShops();        // 2) recharger shops depuis shops.yml
                plugin.getShopManager().restoreOrRotate();  // 3) restaurer (ou tourner si nouveau jour)
                sender.sendMessage("§aConfiguration rechargée sans rotation forcée.");
            }

            case "rotate" -> {
                if (!sender.hasPermission("dailyshop.admin")) {
                    sender.sendMessage("§cPermission refusée.");
                    return true;
                }
                plugin.getShopManager().rotateShop();
                sender.sendMessage("§aShop du jour changé manuellement.");
            }

            case "preview" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande réservée aux joueurs.");
                    return true;
                }
                if (!sender.hasPermission("dailyshop.preview")) {
                    player.sendMessage("§cTu n’as pas la permission d’utiliser cette commande.");
                    return true;
                }

                Shop next = plugin.getShopManager().getNextShop();
                if (next == null) {
                    player.sendMessage("§cAucun shop prévu pour demain.");
                    return true;
                }

                plugin.getMenuManager().openShopMenu(player, next);
            }

            default -> sender.sendMessage("§cCommande inconnue.");
        }

        return true;
    }
}
