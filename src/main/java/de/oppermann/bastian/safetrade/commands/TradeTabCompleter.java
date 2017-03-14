package de.oppermann.bastian.safetrade.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This is class is used for tab completion.
 */
public class TradeTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> list = new LinkedList<>();

        if (args.length == 1) { // only 1 argument is possible atm
            list.add("accept");
            list.add("deny");
            list.add("help");
            list.add("reload");
            for (Player player : Bukkit.getOnlinePlayers()) { // add all online players
                if (!sender.equals(player)) { // except the player itself
                    if (sender instanceof Player) {
                        if (!((Player) sender).canSee(player)) {
                            continue;
                        }
                    }
                    list.add(player.getName());
                }
            }
        }

        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String str = iterator.next().toLowerCase();
            if (!str.startsWith(args[args.length - 1].toLowerCase())) { // don't complete names which don't fit the current input
                iterator.remove();
            }
        }
        return list;
    }

}
