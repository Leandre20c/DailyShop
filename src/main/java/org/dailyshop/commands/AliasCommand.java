package org.dailyshop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AliasCommand implements CommandExecutor {

    private final CommandExecutor delegate;
    private final String[] forcedArgs;

    public AliasCommand(CommandExecutor delegate, String... forcedArgs) {
        this.delegate = delegate;
        this.forcedArgs = forcedArgs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] combined = new String[forcedArgs.length + args.length];
        System.arraycopy(forcedArgs, 0, combined, 0, forcedArgs.length);
        System.arraycopy(args, 0, combined, forcedArgs.length, args.length);

        return delegate.onCommand(sender, command, label, combined);
    }
}
