package de.oppermann.bastian.safetrade.commands;

import de.oppermann.bastian.safetrade.Main;
import de.oppermann.bastian.safetrade.events.TradeRequestAcceptEvent;
import de.oppermann.bastian.safetrade.events.TradeRequestEvent;
import de.oppermann.bastian.safetrade.util.AcceptAction;
import de.oppermann.bastian.safetrade.util.AcceptCommandManager;
import de.oppermann.bastian.safetrade.util.Trade;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class is the executor for the /trade command.
 */
public class TradeCommand implements CommandExecutor {

    /**
     * This HashMap stores the time of the last trade request.
     */
    private final HashMap<UUID, Long> lastRequest = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("wrong_usage").replace("{command}",
                    ChatColor.GOLD + "/trade help" + ChatColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) { // reload the config
            if (!sender.hasPermission("safetrade.reload")) {
                sender.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("no_permission"));
                return true;
            }

            // reload
            Main.getInstance().reload();
            sender.sendMessage(ChatColor.GREEN + Main.getInstance().getMessages().getString("reload_successful"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Main.getInstance().getMessages().getString("players_only"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("safetrade.request")
                && !player.hasPermission("safetrade.accept")
                && !player.hasPermission("safetrade.deny")) {
            sender.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("no_permission"));
            return true;
        }

        if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
            player.sendMessage(ChatColor.BLUE + "---------------------" + ChatColor.RED + " SafeTrade " + ChatColor.BLUE + "---------------------");
            if (player.hasPermission("safetrade.request")) { // only send help is player is allowed to use the command
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.GOLD + Main.getInstance().getMessages().getString("help_command_trade_player"));
                for (String message : Main.getInstance().getMessages().getString("help_command_trade_player_description").split("\n")) {
                    player.sendMessage(ChatColor.ITALIC + " " + message);
                }
            }
            if (player.hasPermission("safetrade.accept")) {
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.GOLD + Main.getInstance().getMessages().getString("help_command_trade_accept"));
                for (String message : Main.getInstance().getMessages().getString("help_command_trade_accept_description").split("\n")) {
                    player.sendMessage(ChatColor.ITALIC + " " + message);
                }
            }
            if (player.hasPermission("safetrade.deny")) {
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.GOLD + Main.getInstance().getMessages().getString("help_command_trade_deny"));
                for (String message : Main.getInstance().getMessages().getString("help_command_trade_deny_description").split("\n")) {
                    player.sendMessage(ChatColor.ITALIC + " " + message);
                }
            }
            if (player.hasPermission("safetrade.reload")) {
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.GOLD + Main.getInstance().getMessages().getString("help_command_trade_reload"));
                for (String message : Main.getInstance().getMessages().getString("help_command_trade_reload_description").split("\n")) {
                    player.sendMessage(ChatColor.ITALIC + " " + message);
                }
            }
            player.sendMessage(ChatColor.BLUE + "-----------------------------------------------------");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) { // accept the trade
            if (!player.hasPermission("safetrade.accept")) {
                sender.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("no_permission"));
                return true;
            }
            AcceptAction action = AcceptCommandManager.getAction(player);

            if (action == null) {
                player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("no_trade_to_accept"));
                return true;
            }

            AcceptCommandManager.finish(player, false);
            action.perform();
            return true;
        }

        if (args[0].equalsIgnoreCase("deny")) { // deny the trade
            if (!player.hasPermission("safetrade.deny")) {
                sender.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("no_permission"));
                return true;
            }
            AcceptAction action = AcceptCommandManager.getAction(player);

            if (action == null) {
                player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("no_trade_to_deny"));
                return true;
            }

            AcceptCommandManager.finish(player, true);
            player.sendMessage(ChatColor.GREEN + Main.getInstance().getMessages().getString("trade_denied"));
            return true;
        }

        if (!player.hasPermission("safetrade.request")) {
            sender.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("no_permission"));
            return true;
        }

        if (lastRequest.containsKey(player.getUniqueId())) {
            if (lastRequest.get(player.getUniqueId()) > System.currentTimeMillis() - 1000 * 10) {
                player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("no_request_spam"));
                return true;
            }
        }

        // request a trade
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("player_not_online").replace("{player}", args[0]));
            return true;
        }

        // Don't allow trades with hidden players
        if (!(player.canSee(target) || Main.getInstance().getConfig().getBoolean("tradeWithHiddenPlayers"))) {
            player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("player_not_online").replace("{player}", args[0]));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("cannot_trade_with_yourself"));
            return true;
        }

        if (!Main.getInstance().getConfig().getBoolean("tradeThroughWorlds") && !player.getWorld().equals(target.getWorld())) {
            player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("player_in_other_world").replace("{player}", target.getName()));
            return true;
        }

        int maxDistance = Main.getInstance().getConfig().getInt("maxTradingDistance");
        if (!Main.getInstance().getConfig().getBoolean("tradeThroughWorlds") && maxDistance > 0 &&
                player.getLocation().distanceSquared(target.getLocation()) > maxDistance * maxDistance) {

            player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("player_to_far_away")
                    .replace("{player}", target.getName())
                    .replace("{max_distance}", String.valueOf(maxDistance)));
            return true;
        }

        lastRequest.put(player.getUniqueId(), System.currentTimeMillis());

        TradeRequestEvent event = new TradeRequestEvent(player, target);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // The event was cancelled
            return true;
        }

        player.sendMessage(ChatColor.GREEN + Main.getInstance().getMessages().getString("successfully_requested_player")
                .replace("{player}", target.getName()));
        target.sendMessage(ChatColor.GREEN + Main.getInstance().getMessages().getString("player_wants_to_trade")
                .replace("{player}", player.getName()));

        target.spigot().sendMessage(generateTextComponents());

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
                    target.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("player_not_online").replace("{player}", playerName));
                    return;
                }
                if (!Main.getInstance().getConfig().getBoolean("tradeThroughWorlds") && !target.getWorld().equals(player.getWorld())) {
                    target.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("player_in_other_world").replace("{player}", player.getName()));
                    return;
                }

                if (player.isSleeping() || target.isSleeping()) {
                    target.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("trading_not_possible_in_bed"));
                    return;
                }

                int maxDistance = Main.getInstance().getConfig().getInt("maxTradingDistance");
                if (!Main.getInstance().getConfig().getBoolean("tradeThroughWorlds") && maxDistance > 0 &&
                        target.getLocation().distanceSquared(player.getLocation()) > maxDistance * maxDistance) {

                    target.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("player_to_far_away")
                            .replace("{player}", player.getName())
                            .replace("{max_distance}", String.valueOf(maxDistance)));
                    return;
                }

                TradeRequestAcceptEvent event = new TradeRequestAcceptEvent(player, target);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    // The event was cancelled
                    return;
                }

                try {
                    new Trade(target, player);
                } catch (IllegalStateException e) {
                    player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("trade_not_possible"));
                }
            }

            @Override
            public void onTimeout() {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("trade_request_not_accepted"));
                }
            }
        });
        return true;
    }

    /**
     * Generates the component for how_to_accept_trade message.
     *
     * @return An array of text components.
     */
    private BaseComponent[] generateTextComponents() {
        ComponentBuilder builder = new ComponentBuilder();
        String howToMessage = Main.getInstance().getMessages().getString("how_to_accept_trade") + " ";
        String[] splitHowToMessage = howToMessage.split("\\{command}");
        for (int i = 0; i < splitHowToMessage.length - 1; i++) {
            builder.append(splitHowToMessage[i]).color(net.md_5.bungee.api.ChatColor.GREEN);
            BaseComponent[] hoverComponents = new ComponentBuilder()
                    .append(Main.getInstance().getMessages().getString("how_to_accept_trade_command_hover")).create();
            builder.append("/trade accept").color(net.md_5.bungee.api.ChatColor.GOLD)
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trade accept"))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverComponents)));
        }
        builder.append(splitHowToMessage[splitHowToMessage.length - 1]).color(net.md_5.bungee.api.ChatColor.GREEN);
        return builder.create();
    }

}
