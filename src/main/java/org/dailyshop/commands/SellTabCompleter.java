package org.dailyshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SellTabCompleter implements TabCompleter {

    private static final List<String> OPTIONS = Arrays.asList("hand", "handall", "all");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return OPTIONS.stream()
                    .filter(opt -> opt.startsWith(input))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
