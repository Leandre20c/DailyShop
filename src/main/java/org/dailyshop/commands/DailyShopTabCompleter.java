package org.dailyshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DailyShopTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("open", "menu", "giveitem", "reload", "rotate", "preview");
    private static final List<String> GIVEITEM_ARGS = List.of("sell");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1 -> {
                for (String sub : SUBCOMMANDS) {
                    if (sub.startsWith(args[0].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            }

            case 2 -> {
                if (args[0].equalsIgnoreCase("giveitem")) {
                    for (String arg : GIVEITEM_ARGS) {
                        if (arg.startsWith(args[1].toLowerCase())) {
                            completions.add(arg);
                        }
                    }
                }
            }

            case 3 -> {
                if (args[0].equalsIgnoreCase("giveitem") && args[1].equalsIgnoreCase("sell")) {
                    completions.add("<uses>");
                }
            }
        }

        return completions;
    }
}
