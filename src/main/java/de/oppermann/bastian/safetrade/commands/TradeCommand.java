package de.oppermann.bastian.safetrade.commands;

import de.oppermann.bastian.safetrade.Main;
import de.oppermann.bastian.safetrade.util.AcceptAction;
import de.oppermann.bastian.safetrade.util.AcceptCommandManager;
import de.oppermann.bastian.safetrade.util.JSONUtil;
import de.oppermann.bastian.safetrade.util.Trade;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * This class is the executor for the /trade command.
 */
public class TradeCommand implements CommandExecutor {

    /**
     * The {@link ResourceBundle} which contains all messages.
     */
    private ResourceBundle messages = Main.getInstance().getMessages();

    /**
     * This HashMap stores the time of the last trade request.
     */
    private final HashMap<UUID, Long> lastRequest = new HashMap<>();

    /*
     * (non-Javadoc)
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getString("players_only"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("safetrade.request")
                && !player.hasPermission("safetrade.accept")
                && !player.hasPermission("safetrade.deny")) {
            sender.sendMessage(ChatColor.RED + messages.getString("no_permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + messages.getString("wrong_usage").replace("{command}",
                    ChatColor.GOLD + "/trade help" + ChatColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
            player.sendMessage(ChatColor.BLUE + "---------------------" + ChatColor.RED + " SafeTrade " + ChatColor.BLUE + "---------------------");
            if (player.hasPermission("safetrade.request")) { // only send help is player is allowed to use the command
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.GOLD + messages.getString("help_command_trade_player"));
                for (String message : messages.getString("help_command_trade_player_description").split("\n")) {
                    player.sendMessage(ChatColor.ITALIC + " " + message);
                }
            }
            if (player.hasPermission("safetrade.accept")) {
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.GOLD + messages.getString("help_command_trade_accept"));
                for (String message : messages.getString("help_command_trade_accept_description").split("\n")) {
                    player.sendMessage(ChatColor.ITALIC + " " + message);
                }
            }
            if (player.hasPermission("safetrade.deny")) {
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.GOLD + messages.getString("help_command_trade_deny"));
                for (String message : messages.getString("help_command_trade_deny_description").split("\n")) {
                    player.sendMessage(ChatColor.ITALIC + " " + message);
                }
            }
            player.sendMessage(ChatColor.BLUE + "-----------------------------------------------------");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) { // accept the trade
            if (!player.hasPermission("safetrade.accept")) {
                sender.sendMessage(ChatColor.RED + messages.getString("no_permission"));
                return true;
            }
            AcceptAction action = AcceptCommandManager.getAction(player);

            if (action == null) {
                player.sendMessage(ChatColor.RED + messages.getString("no_trade_to_accept"));
                return true;
            }

            AcceptCommandManager.finish(player, false);
            action.perform();
            return true;
        }

        if (args[0].equalsIgnoreCase("deny")) { // deny the trade
            if (!player.hasPermission("safetrade.deny")) {
                sender.sendMessage(ChatColor.RED + messages.getString("no_permission"));
                return true;
            }
            AcceptAction action = AcceptCommandManager.getAction(player);

            if (action == null) {
                player.sendMessage(ChatColor.RED + messages.getString("no_trade_to_deny"));
                return true;
            }

            AcceptCommandManager.finish(player, true);
            player.sendMessage(ChatColor.GREEN + messages.getString("trade_denied"));
            return true;
        }

        if (!player.hasPermission("safetrade.request")) {
            sender.sendMessage(ChatColor.RED + messages.getString("no_permission"));
            return true;
        }

        if (lastRequest.containsKey(player.getUniqueId())) {
            if (lastRequest.get(player.getUniqueId()) > System.currentTimeMillis() - 1000 * 10) {
                player.sendMessage(ChatColor.RED + messages.getString("no_request_spam"));
                return true;
            }
        }

        // request a trade
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + messages.getString("player_not_online").replace("{player}", args[0]));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + messages.getString("cannot_trade_with_yourself"));
            return true;
        }

        if (!Main.getInstance().getConfig().getBoolean("tradeThroughWorlds") && !player.getWorld().equals(target.getWorld())) {
            player.sendMessage(ChatColor.RED + messages.getString("player_in_other_world").replace("{player}", target.getName()));
            return true;
        }

        int maxDistance = Main.getInstance().getConfig().getInt("maxTradingDistance");
        if (!Main.getInstance().getConfig().getBoolean("tradeThroughWorlds") && maxDistance > 0 &&
                player.getLocation().distanceSquared(target.getLocation()) > maxDistance * maxDistance) {

            player.sendMessage(ChatColor.RED + messages.getString("player_to_far_away")
                    .replace("{player}", target.getName())
                    .replace("{max_distance}", String.valueOf(maxDistance)));
            return true;
        }

        lastRequest.put(player.getUniqueId(), System.currentTimeMillis());

        player.sendMessage(ChatColor.GREEN + messages.getString("successfully_requested_player")
                .replace("{player}", target.getName()));
        target.sendMessage(ChatColor.GREEN + messages.getString("player_wants_to_trade")
                .replace("{player}", player.getName()));


        if (!JSONUtil.sendJSONText(target, generateJSONString())) { // if something failed
            target.sendMessage(ChatColor.GREEN + messages.getString("how_to_accept_trade")
                    .replace("{command}", ChatColor.GOLD + "/trade accept" + ChatColor.GREEN));
        }

        final UUID targetUUID = target.getUniqueId();
        final String playerName = player.getName();
        final UUID playerUUID = player.getUniqueId();
        // now let's wait that the other player accepts the trade :)
        AcceptCommandManager.addAction(target, new AcceptAction() {

            @Override
            public void perform() {
                Player player = Bukkit.getPlayer(playerUUID);
                Player target = Bukkit.getPlayer(targetUUID);
                if (player == null) { // (target cannot be offline)
                    target.sendMessage(ChatColor.RED + messages.getString("player_not_online").replace("{player}", playerName));
                    return;
                }
                if (!Main.getInstance().getConfig().getBoolean("tradeThroughWorlds") && !target.getWorld().equals(player.getWorld())) {
                    target.sendMessage(ChatColor.RED + messages.getString("player_in_other_world").replace("{player}", player.getName()));
                    return;
                }

                int maxDistance = Main.getInstance().getConfig().getInt("maxTradingDistance");
                if (!Main.getInstance().getConfig().getBoolean("tradeThroughWorlds") && maxDistance > 0 &&
                        target.getLocation().distanceSquared(player.getLocation()) > maxDistance * maxDistance) {

                    target.sendMessage(ChatColor.RED + messages.getString("player_to_far_away")
                            .replace("{player}", player.getName())
                            .replace("{max_distance}", String.valueOf(maxDistance)));
                    return;
                }

                try {
                    new Trade(target, player);
                } catch (IllegalStateException e) {
                    player.sendMessage(ChatColor.RED + messages.getString("trade_not_possible"));
                }
            }

            @Override
            public void onTimeout() {
                Player player = Bukkit.getPlayer(playerUUID);
                player.sendMessage(ChatColor.RED + messages.getString("trade_request_not_accepted"));
            }
        });
        return true;
    }

    /**
     * Generates the json string for how_to_accept_trade.
     *
     * @return A json string.
     */
    private String generateJSONString() {
        StringBuilder jsonMessage = new StringBuilder();
        String howToMessage = messages.getString("how_to_accept_trade");
        ArrayList<String> list = new ArrayList<>();
        while (howToMessage.contains("{command}")) {
            list.add(howToMessage.substring(0, howToMessage.indexOf("{command}")));
            list.add("{command}");
            howToMessage = howToMessage.substring(9 + howToMessage.indexOf("{command}"), howToMessage.length());
        }
        list.add(howToMessage);
        jsonMessage.append("[\"\"");
        for (String str : list) {
            if (str.equals("{command}")) {
                jsonMessage.append(",{\"text\":\"/trade accept\",\"color\":\"gold\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/trade accept\"}}");
            } else {
                str = str.replace("\"", "");
                jsonMessage.append(",{\"text\":\"" + str + "\",\"color\":\"green\"}");
            }
        }
        jsonMessage.append("]");
        return jsonMessage.toString();
    }

}
