package de.oppermann.bastian.safetrade.util;

import de.oppermann.bastian.safetrade.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

/**
 * This class manages the trades.
 */
public class Trade {

    /**
     * A list which includes all active trades.
     */
    private static final List<Trade> activeTrades = new ArrayList<>();

    /**
     * The two traders.
     */
    private final UUID[] traders = new UUID[2];

    /**
     * The two trading inventories.
     */
    private final Inventory[] tradingInventories = new Inventory[2];

    /**
     * The array shows which player is ready or has accepted the trade.
     */
    private final boolean[] acceptedOrReady = {false, false};

    /**
     * The money which the players offers.
     */
    private final int offeredMoney[] = {0, 0};

    /**
     * The status of the trade.
     * <p><code>false</code> = waiting for 'ready'
     * <p><code>true</code> = waiting for 'accept'
     */
    private boolean status;

    /**
     * A copy of the IEconomy inctance in the main class.<p>
     * We don't want that some strange things happen if the field changed while trading (worst case: null).
     */
    private IEconomy economy = Main.getInstance().getEconomy();

    /**
     * Class constructor.
     *
     * @param player1 The first player.
     * @param player2 The second player.
     */
    public Trade(Player player1, Player player2) {
        traders[0] = player1.getUniqueId();
        traders[1] = player2.getUniqueId();

        tradingInventories[0] = Main.getInstance().getInventoryUtil().createInventory(economy != null, player1, player2);
        tradingInventories[1] = Main.getInstance().getInventoryUtil().createInventory(economy != null, player2, player1);

        if (getTradeOf(player1) != null || getTradeOf(player2) != null) {
            throw new IllegalStateException("One player (or both) is already trading with someone!");
        }
        activeTrades.add(this);

        player1.openInventory(tradingInventories[0]);
        player2.openInventory(tradingInventories[1]);
    }

    /**
     * Marks a player as 'ready' or 'accepted trade'.
     *
     * @param player The player.
     */
    public void approve(Player player) {
        // some kind of fail-safe if there was an error
        Main.getInstance().getInventoryUtil().synchronize(tradingInventories[0], tradingInventories[1], economy != null);
        // with the events/listener
        Main.getInstance().getInventoryUtil().synchronize(tradingInventories[1], tradingInventories[0], economy != null);

        if (traders[0].equals(player.getUniqueId())) {
            acceptedOrReady[0] = true;
            Main.getInstance().getInventoryUtil().setPartnerStatus(tradingInventories[1], true,
                    ChatColor.GREEN + (!status ? Main.getInstance().getMessages().getString("partner_ready") :
                            Main.getInstance().getMessages().getString("partner_accepted_trade")));
            Main.getInstance().getInventoryUtil().setOwnControlField(tradingInventories[0], status ? (byte) 3 : (byte) 1, economy != null);
        } else {
            acceptedOrReady[1] = true;
            Main.getInstance().getInventoryUtil().setPartnerStatus(tradingInventories[0], true,
                    ChatColor.GREEN + (!status ? Main.getInstance().getMessages().getString("partner_ready") :
                            Main.getInstance().getMessages().getString("partner_accepted_trade")));
            Main.getInstance().getInventoryUtil().setOwnControlField(tradingInventories[1], status ? (byte) 3 : (byte) 1, economy != null);
        }
        Bukkit.getPlayer(traders[0]).updateInventory();
        Bukkit.getPlayer(traders[1]).updateInventory();
        if (!status && acceptedOrReady[0] && acceptedOrReady[1]) {
            status = true; // next step! Now they are both ready and must accept the trade :)
            acceptedOrReady[0] = false;
            acceptedOrReady[1] = false;
            Main.getInstance().getInventoryUtil().setPartnerStatus(tradingInventories[0], false,
                    ChatColor.RED + Main.getInstance().getMessages().getString("partner_not_accepted_yet"));
            Main.getInstance().getInventoryUtil().setPartnerStatus(tradingInventories[1], false,
                    ChatColor.RED + Main.getInstance().getMessages().getString("partner_not_accepted_yet"));
            Main.getInstance().getInventoryUtil().setOwnControlField(tradingInventories[0], (byte) 2, economy != null);
            Main.getInstance().getInventoryUtil().setOwnControlField(tradingInventories[1], (byte) 2, economy != null);
            Bukkit.getPlayer(traders[0]).updateInventory();
            Bukkit.getPlayer(traders[1]).updateInventory();
        }
        if (status && acceptedOrReady[0] && acceptedOrReady[1]) {
            // remove this first or trade will be aborted because of closing inventory
            activeTrades.remove(this);

            Player player1 = Bukkit.getPlayer(traders[0]);
            Player player2 = Bukkit.getPlayer(traders[1]);

            player1.closeInventory();
            player2.closeInventory(); // I think it's safer to close the inventories BEFORE giving them their items

            if (economy != null && (
               (economy.getMoney(player1) < offeredMoney[0] && offeredMoney[0] != 0) ||
               (economy.getMoney(player2) < offeredMoney[1] && offeredMoney[1] != 0))
            ) { // If a player hasn't enough money.
                if (Main.getInstance().getConfig().getBoolean("noDebts", true)) {
                    for (int slot : Main.getInstance().getInventoryUtil().TRADING_SLOTS_LEFT_WITH_MONEY) {
                        ItemStack stack = tradingInventories[0].getItem(slot);
                        if (stack != null) {
                            giveItem(player1, stack);
                        }
                        stack = tradingInventories[1].getItem(slot);
                        if (stack != null) {
                            giveItem(player2, stack);
                        }
                    }
                    if (economy.getMoney(player1) < offeredMoney[0] && offeredMoney[0] != 0) {
                        player1.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("not_enough_money_you"));
                        player2.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("not_enough_money_partner")
                                .replace("{player}", player1.getName()));
                    } else {
                        player2.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("not_enough_money_you"));
                        player1.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("not_enough_money_partner")
                                .replace("{player}", player2.getName()));
                    }
                    return;
                }
            }
            for (int slot : economy != null ?
                    Main.getInstance().getInventoryUtil().TRADING_SLOTS_LEFT_WITH_MONEY : Main.getInstance().getInventoryUtil().TRADING_SLOTS_LEFT_WITHOUT_MONEY) {
                ItemStack stack = tradingInventories[0].getItem(slot);
                if (stack != null) {
                    giveItem(player2, stack); // the same as abort but with swapped players :)
                }
                stack = tradingInventories[1].getItem(slot);
                if (stack != null) {
                    giveItem(player1, stack);
                }
            }

            if (economy != null) {
                if (offeredMoney[0] != 0) {
                    economy.withdrawMoney(player1, offeredMoney[0]);
                    economy.depositMoney(player2, offeredMoney[0]);
                }
                if (offeredMoney[1] != 0) {
                    economy.withdrawMoney(player2, offeredMoney[1]);
                    economy.depositMoney(player1, offeredMoney[1]);
                }
            }

            player1.sendMessage(ChatColor.GREEN + Main.getInstance().getMessages().getString("trade_succeeded"));
            player2.sendMessage(ChatColor.GREEN + Main.getInstance().getMessages().getString("trade_succeeded"));

            Main.getInstance().incrementSuccessfulTrades();
        }
    }

    /**
     * Aborts a trade (e.g. because of closing inventory or pressing the abort-button).
     *
     * @param whoAborted The player who aborted the trade.
     */
    public void abort(Player whoAborted) {
        // remove this first or you will be stuck in an endless loop (InventoryCloseListener also executed this method)
        activeTrades.remove(this);

        Player player1 = Bukkit.getPlayer(traders[0]);
        Player player2 = Bukkit.getPlayer(traders[1]);

        player1.getInventory().addItem(player1.getItemOnCursor());
        player1.setItemOnCursor(null);
        player2.getInventory().addItem(player2.getItemOnCursor());
        player2.setItemOnCursor(null);

        player1.closeInventory();
        player2.closeInventory(); // I think it's safer to close the inventories BEFORE giving them back their items

        for (int slot : economy != null ?
                Main.getInstance().getInventoryUtil().TRADING_SLOTS_LEFT_WITH_MONEY : Main.getInstance().getInventoryUtil().TRADING_SLOTS_LEFT_WITHOUT_MONEY) {
            ItemStack stack = tradingInventories[0].getItem(slot);
            if (stack != null) {
                giveItem(player1, stack);
            }
            stack = tradingInventories[1].getItem(slot);
            if (stack != null) {
                giveItem(player2, stack);
            }
        }

        if (whoAborted != null && player1.equals(whoAborted)) {
            player2.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("player_aborted_trade")
                    .replace("{player}", whoAborted.getName()));
            player1.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("you_aborted_trade"));
        } else if (whoAborted != null) {
            player1.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("player_aborted_trade")
                    .replace("{player}", whoAborted.getName()));
            player2.sendMessage(ChatColor.RED + Main.getInstance().getMessages().getString("you_aborted_trade"));
        }
        Main.getInstance().incrementAbortedTrades();
    }

    /**
     * Gets the status of the trade.
     * <p><code>false</code> = waiting for 'ready'
     * <p><code>true</code> = waiting for 'accept'
     *
     * @return The status of the trade.
     */
    public boolean getStatus() {
        return status;
    }

    /**
     * Checks if the player is ready or has accepted the trade.
     *
     * @param player The player.
     * @return If the player is ready or has accepted the trade.
     */
    public boolean isReadyOrHasAccepted(UUID player) {
        if (traders[0].equals(player)) {
            return acceptedOrReady[0];
        } else {
            return acceptedOrReady[1];
        }
    }

    /**
     * Gets the trading inventory of player's trading partner.
     *
     * @param player The player.
     * @return The trading inventory of player's trading partner.
     */
    public Inventory getInventoryOfPartner(UUID player) {
        if (traders[0].equals(player)) {
            return tradingInventories[1];
        } else {
            return tradingInventories[0];
        }
    }

    /**
     * Gets the current slots the player has to click to accept a trade or mark his self as ready.
     *
     * @param player The player.
     * @return The current slots the player has to click to accept a trade or mark his self as ready.
     */
    public List<Integer> getCurrentAcceptSlots(UUID player) {
        byte invType = (byte) 0;
        if (traders[0].equals(player)) {
            invType += status ? (byte) 2 : (byte) 0;
            invType += acceptedOrReady[0] ? (byte) 1 : (byte) 0;
        } else {
            invType += status ? (byte) 2 : (byte) 0;
            invType += acceptedOrReady[1] ? (byte) 1 : (byte) 0;
        }

        switch (invType) {
            case 0:
                return economy == null ?
                        Arrays.asList(9 * 4 + 0, 9 * 4 + 1, 9 * 5 + 0, 9 * 5 + 1) : Arrays.asList(9 * 5 + 0, 9 * 5 + 1);
            case 2:
                return Arrays.asList(9 * 4 + 0, 9 * 4 + 1, 9 * 5 + 0, 9 * 5 + 1);
            default:
                return Arrays.asList();
        }
    }

    /**
     * Gets the current slots the player has to click to abort the trade.
     *
     * @param player The player.
     * @return The current slots the player has to click to abort the trade.
     */
    public List<Integer> getCurrentAbortSlots(UUID player) {
        byte invType = (byte) 0;
        if (traders[0].equals(player)) {
            invType += status ? (byte) 2 : (byte) 0;
            invType += acceptedOrReady[0] ? (byte) 1 : (byte) 0;
        } else {
            invType += status ? (byte) 2 : (byte) 0;
            invType += acceptedOrReady[1] ? (byte) 1 : (byte) 0;
        }

        switch (invType) {
            case 0:
                return economy == null ?
                        Arrays.asList(9 * 4 + 2, 9 * 4 + 3, 9 * 5 + 2, 9 * 5 + 3) : Arrays.asList(9 * 5 + 2, 9 * 5 + 3);
            case 2:
                return Arrays.asList(9 * 4 + 2, 9 * 4 + 3, 9 * 5 + 2, 9 * 5 + 3);
            default:
                return Arrays.asList();
        }
    }

    /**
     * Gets the current slots the player is allowed to modify (place/move/remove items).
     *
     * @param player The player.
     * @return The current slots the player is allowed to modify (place/move/remove items).
     */
    public List<Integer> getCurrentAllowedSlots(UUID player) {
        if (status || isReadyOrHasAccepted(player)) {
            return Arrays.asList();
        }
        if (economy == null) {
            return Arrays.asList(9 * 0 + 0, 9 * 0 + 1, 9 * 0 + 2, 9 * 0 + 3,
                                 9 * 1 + 0, 9 * 1 + 1, 9 * 1 + 2, 9 * 1 + 3,
                                 9 * 2 + 0, 9 * 2 + 1, 9 * 2 + 2, 9 * 2 + 3);
        } else {
            return Arrays.asList(9 * 0 + 0, 9 * 0 + 1, 9 * 0 + 2, 9 * 0 + 3,
                                 9 * 1 + 0, 9 * 1 + 1, 9 * 1 + 2, 9 * 1 + 3,
                                 9 * 2 + 0, 9 * 2 + 1, 9 * 2 + 2);
        }

    }

    /**
     * Changes the amount of offered money.
     *
     * @param player       The player
     * @param increaseType The increase type.
     *                     <p><code>0</code> = Increase or decrease by 'smallMoneyValue'.
     *                     <p><code>1</code> = Increase or decrease by 'mediumMoneyValue'.
     *                     <p><code>2</code> = Increase or decrease by 'largeMoneyValue'.
     *                     <p><code>3</code> = Set money to 0.
     * @param increase     Whether the money should be added or removed.
     */
    public void changeMoney(UUID player, byte increaseType, boolean increase) {
        int traderId = 0;
        if (!traders[0].equals(player)) {
            traderId = 1;
        }

        switch (increaseType) {
            case 0:
                offeredMoney[traderId] += Main.getInstance().getConfig().getInt("smallMoneyValue") * (increase ? 1 : -1);
                break;
            case 1:
                offeredMoney[traderId] += Main.getInstance().getConfig().getInt("mediumMoneyValue") * (increase ? 1 : -1);
                break;
            case 2:
                offeredMoney[traderId] += Main.getInstance().getConfig().getInt("largeMoneyValue") * (increase ? 1 : -1);
                break;
            default:
                offeredMoney[traderId] = 0;
                break;
        }
        if (offeredMoney[traderId] < 0) { // no negative values
            offeredMoney[traderId] = 0;
        }

        Main.getInstance().getInventoryUtil().setMoney(tradingInventories[traderId], offeredMoney[traderId], true);
        Main.getInstance().getInventoryUtil().setMoney(tradingInventories[traderId == 0 ? 1 : 0], offeredMoney[traderId], false);
        Bukkit.getPlayer(traders[0]).updateInventory();
        Bukkit.getPlayer(traders[1]).updateInventory();
    }

    /**
     * Gets the slots the player has to click to increase or decrease the money.
     *
     * @param player       The player.
     * @param increaseType The increase type.
     *                     <p><code>0</code> = Increase or decrease by 'smallMoneyValue'.
     *                     <p><code>1</code> = Increase or decrease by 'mediumMoneyValue'.
     *                     <p><code>2</code> = Increase or decrease by 'largeMoneyValue'.
     *                     <p><code>3</code> = Set money to 0.
     * @return The slots to click at.
     */
    public List<Integer> getCurrentIncreaseMoneySlot(UUID player, byte increaseType) {
        if (economy == null) { // no economy plugin = no trading
            return Arrays.asList();
        }
        byte invType = (byte) 0;
        if (traders[0].equals(player)) {
            invType += status ? (byte) 2 : (byte) 0;
            invType += acceptedOrReady[0] ? (byte) 1 : (byte) 0;
        } else {
            invType += status ? (byte) 2 : (byte) 0;
            invType += acceptedOrReady[1] ? (byte) 1 : (byte) 0;
        }

        if (invType == 0) {
            switch (increaseType) {
                case 0:
                    return Arrays.asList(9 * 4 + 0);
                case 1:
                    return Arrays.asList(9 * 4 + 1);
                case 2:
                    return Arrays.asList(9 * 4 + 2);
                case 3:
                    return Arrays.asList(9 * 4 + 3);
            }
        }
        return Arrays.asList();
    }

    /**
     * Gives a player his item. If the player has not enough space the item is dropped on the ground.
     *
     * @param player The player.
     * @param stack The stack.
     */
    private void giveItem(Player player, ItemStack stack) {
        HashMap<Integer, ItemStack> notFitting = player.getInventory().addItem(stack);
        for (ItemStack is : notFitting.values()) {
            Item item = player.getWorld().dropItem(player.getLocation(), is);
            // Add a metadata tag for the dropped item (it's used to identify the "owner" of the item)
            item.setMetadata("drop_timestamp", new FixedMetadataValue(Main.getInstance(), System.currentTimeMillis()));
            item.setMetadata("item_owner", new FixedMetadataValue(Main.getInstance(), player.getName()));
        }
    }

    /**
     * Gets a copy of the list of active threads.
     *
     * @return A list of all active threads.
     */
    public static List<Trade> getActiveTrades() {
        return new ArrayList<>(activeTrades); // we don't want that someone modifies our original list, do we?
    }

    /**
     * Searches for the trades of a player.
     *
     * @param player The player to search.
     * @return The active trade of the player. <code>null</code> if no trade was found.
     */
    public static Trade getTradeOf(Player player) {
        UUID playerUniqueId = player.getUniqueId();
        for (Trade trade : activeTrades) {
            if (trade.traders[0].equals(playerUniqueId) || trade.traders[1].equals(playerUniqueId)) {
                return trade;
            }
        }
        return null;
    }

}
