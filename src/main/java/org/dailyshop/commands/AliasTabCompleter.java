package org.dailyshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

public class AliasTabCompleter implements TabCompleter {

    private final TabCompleter delegate;
    private final String[] forcedArgs;

    public AliasTabCompleter(TabCompleter delegate, String... forcedArgs) {
        this.delegate = delegate;
        this.forcedArgs = forcedArgs;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String[] combined = new String[forcedArgs.length + args.length];
        System.arraycopy(forcedArgs, 0, combined, 0, forcedArgs.length);
        System.arraycopy(args, 0, combined, forcedArgs.length, args.length);

        return delegate.onTabComplete(sender, command, alias, combined);
    }
}
