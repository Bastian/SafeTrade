package de.oppermann.bastian.safetrade.util;

import de.oppermann.bastian.safetrade.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class includes some useful function related to trade inventories.
 */
@SuppressWarnings("PointlessArithmeticExpression")
public class InventoryUtil {

    private final Design design;

    public InventoryUtil(Design design) {
        this.design = design;
    }

    /**
     * All allowed slots to place/move/etc. items.
     */
    public final static List<Integer> TRADING_SLOTS_LEFT_WITH_MONEY = Collections.unmodifiableList(
            Arrays.asList(9 * 0 + 0, 9 * 0 + 1, 9 * 0 + 2, 9 * 0 + 3,
                          9 * 1 + 0, 9 * 1 + 1, 9 * 1 + 2, 9 * 1 + 3,
                          9 * 2 + 0, 9 * 2 + 1, 9 * 2 + 2));

    /**
     * All allowed slots to place/move/etc. items.
     */
    public final static List<Integer> TRADING_SLOTS_LEFT_WITHOUT_MONEY = Collections.unmodifiableList(
            Arrays.asList(9 * 0 + 0, 9 * 0 + 1, 9 * 0 + 2, 9 * 0 + 3,
                          9 * 1 + 0, 9 * 1 + 1, 9 * 1 + 2, 9 * 1 + 3,
                          9 * 2 + 0, 9 * 2 + 1, 9 * 2 + 2, 9 * 2 + 3));

    /**
     * The control-field of the partner (right side).
     */
    public final static List<Integer> CONTROL_FIELD_SLOTS_RIGHT = Collections.unmodifiableList(
            Arrays.asList(9 * 4 + 5, 9 * 4 + 6, 9 * 4 + 7, 9 * 4 + 8,
                          9 * 5 + 5, 9 * 5 + 6, 9 * 5 + 7, 9 * 5 + 8));

    /**
     * Creates a new trading inventory.
     *
     * @param player The inventory holder.
     * @param partner The trading partner.
     * @return A new trading inventory.
     */
    public Inventory createInventory(Player player, Player partner) {
        return createInventory(false, player, partner);
    }

    /**
     * Creates a new trading inventory.
     *
     * @param tradeWithMoney Should it be allowed to trade with money?
     * @param player The inventory holder.
     * @param partner The trading partner.
     * @return A new trading inventory.
     */
    public Inventory createInventory(boolean tradeWithMoney, Player player, Player partner) {
        String title = Main.getInstance().getMessages().getString("tradinginventory_title");
        title = title.replace("{player}", player.getName());
        title = title.replace("{partner}", partner.getName());
        title = title.length() > 32 ? title.substring(0, 32) : title;
        Inventory defaultTradeInventory = Bukkit.createInventory(null, 9 * 6, title);

        ItemStack separateStack = design.getItem("seperator", ChatColor.GOLD.toString());

        for (int i = 0; i < 6; i++) {
            defaultTradeInventory.setItem(9 * i + 4, separateStack.clone());
        }
        for (int i = 0; i < 9; i++) {
            defaultTradeInventory.setItem(9 * 3 + i, separateStack.clone());
        }

        setPartnerStatus(defaultTradeInventory, false, false, ChatColor.RED +
                Main.getInstance().getMessages().getString("partner_not_ready"));


        if (tradeWithMoney) {
            setMoney(defaultTradeInventory, 0, true);
            setMoney(defaultTradeInventory, 0, false);
        }

        setOwnControlField(defaultTradeInventory, (byte) 0, tradeWithMoney);
        return defaultTradeInventory;
    }

    /**
     * Copies the content on the left side of one inventory to the right side of the other inventory.
     *
     * @param copyFrom       The inventory copied from.
     * @param copyTo         The inventory copied to.
     * @param tradeWithMoney Should it be allowed to trade with money?
     */
    public void synchronize(Inventory copyFrom, Inventory copyTo, boolean tradeWithMoney) {
        for (int slot : tradeWithMoney ? TRADING_SLOTS_LEFT_WITH_MONEY : TRADING_SLOTS_LEFT_WITHOUT_MONEY) {
            copyTo.setItem(slot + 5, copyFrom.getItem(slot));
        }
    }

    /**
     * Sets the status of the partner.
     *
     * @param inventory The inventory to modify.
     * @param status    The status (<code>false</code> = 1. stage of the trade, <code>true</code> = 2. stage of the trade).
     * @param done      The state (<code>true</code> = ready/accepted).
     * @param text      The text of the status item.
     */
    public void setPartnerStatus(Inventory inventory, boolean status, boolean done, String text) {
        ItemStack partnerStatus;
        if (status) { // First stage of the trade
            if (done) {
                partnerStatus = design.getItem("partnerAccepted", text);
            } else {
                partnerStatus = design.getItem("partnerNotAccepted", text);
            }
        } else { // Second stage of the trade
            if (done) {
                partnerStatus = design.getItem("partnerReady", text);
            } else {
                partnerStatus = design.getItem("partnerNotReady", text);
            }
        }
        for (int i = 0; i < 4; i++) {
            inventory.setItem(9 * 4 + i + 5, partnerStatus.clone());
            inventory.setItem(9 * 5 + i + 5, partnerStatus.clone());
        }
    }

    /**
     * Sets the money in the inventory.
     *
     * @param inventory The inventory to modify.
     * @param money     The money to set.
     * @param leftSide  Whether the right or the left side should be modified.
     */
    public void setMoney(Inventory inventory, int money, boolean leftSide) {
        int slot = leftSide ? 9 * 2 + 3 : 9 * 2 + 8;

        String type = "veryVeryLargeMoneyOffered";
        if (money < Main.getInstance().getConfig().getInt("largeMoneyValue") * 100) {
            type = "veryLargeMoneyOffered";
        }
        if (money < Main.getInstance().getConfig().getInt("largeMoneyValue") * 10) {
            type = "largeMoneyOffered";
        }
        if (money < Main.getInstance().getConfig().getInt("largeMoneyValue")) {
            type = "mediumMoneyOffered";
        }
        if (money < Main.getInstance().getConfig().getInt("mediumMoneyValue")) {
            type = "smallMoneyOffered";
        }
        if (money <= 0) {
            type = "noMoneyOffered";
        }
        String strMoney = String.valueOf(money);
        if (Main.getInstance().getEconomy() != null) {
            strMoney = Main.getInstance().getEconomy().format(money);
        }
        ItemStack itemStack = design.getItem(type,
                Main.getInstance().getMessages().getString("offered_money").replace("{money}", strMoney));
        if (!type.equals("noMoneyOffered")) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemStack.setItemMeta(itemMeta);
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        inventory.setItem(slot, itemStack);
    }

    /**
     * Sets the status of the partner.
     *
     * @param inventory      The inventory to modify.
     * @param type           Which type of control field should be shown?
     *                       <p><code>0</code> is the default control field.
     *                       <p><code>1</code> means 'ready and waiting for partner'.
     *                       <p><code>2</code> means 'waiting for accept'.
     *                       <p><code>3</code> means 'accepted trade and waiting for partner'
     * @param tradeWithMoney Should it be allowed to trade with money?
     */
    public void setOwnControlField(Inventory inventory, byte type, boolean tradeWithMoney) {
        if (type == 0) {
            String[] readyLore = Main.getInstance().getMessages().getString("button_ready_description").split("\n");
            for (int i = 0; i < readyLore.length; i++) {
                readyLore[i] = ChatColor.GRAY + readyLore[i];
            }
            ItemStack readyButton = design.getItem("markAsReady",
                    ChatColor.DARK_GREEN + Main.getInstance().getMessages().getString("button_ready"), readyLore);

            String[] abortLore = Main.getInstance().getMessages().getString("button_abort_description").split("\n");
            for (int i = 0; i < abortLore.length; i++) {
                abortLore[i] = ChatColor.GRAY + abortLore[i];
            }
            ItemStack abortButton = design.getItem("abortTrade",
                    ChatColor.RED + Main.getInstance().getMessages().getString("button_abort"), abortLore);

            String[] addMoneyLore = Main.getInstance().getMessages().getString("button_add_money_description").split("\n");
            for (int i = 0; i < addMoneyLore.length; i++) {
                addMoneyLore[i] = ChatColor.GRAY + addMoneyLore[i];
            }

            String strMoney = String.valueOf(Main.getInstance().getConfig().getInt("smallMoneyValue"));
            if (Main.getInstance().getEconomy() != null) {
                strMoney = Main.getInstance().getEconomy().format(Main.getInstance().getConfig().getInt("smallMoneyValue"));
            }

            ItemStack addOneMoney = design.getItem("smallMoney", ChatColor.GOLD +
                    Main.getInstance().getMessages().getString("button_add_money").replace("{money}", strMoney), addMoneyLore);

            strMoney = String.valueOf(Main.getInstance().getConfig().getInt("mediumMoneyValue"));
            if (Main.getInstance().getEconomy() != null) {
                strMoney = Main.getInstance().getEconomy().format(Main.getInstance().getConfig().getInt("mediumMoneyValue"));
            }
            ItemStack addTenMoney = design.getItem("mediumMoney", ChatColor.GOLD +
                    Main.getInstance().getMessages().getString("button_add_money").replace("{money}", strMoney), addMoneyLore);

            strMoney = String.valueOf(Main.getInstance().getConfig().getInt("largeMoneyValue"));
            if (Main.getInstance().getEconomy() != null) {
                strMoney = Main.getInstance().getEconomy().format(Main.getInstance().getConfig().getInt("largeMoneyValue"));
            }
            ItemStack addHundredMoney = design.getItem("largeMoney", ChatColor.GOLD +
                            Main.getInstance().getMessages().getString("button_add_money").replace("{money}", strMoney), addMoneyLore);

            String[] clearMoneyLore = Main.getInstance().getMessages().getString("button_clear_money_description").split("\n");
            for (int i = 0; i < clearMoneyLore.length; i++) {
                clearMoneyLore[i] = ChatColor.GRAY + clearMoneyLore[i];
            }
            ItemStack clearMoney = design.getItem("clearMoney",
                    ChatColor.RED + Main.getInstance().getMessages().getString("button_clear_money"), clearMoneyLore);

            inventory.setItem(9 * 4 + 0, tradeWithMoney ? addOneMoney : readyButton);
            inventory.setItem(9 * 4 + 1, tradeWithMoney ? addTenMoney : readyButton);
            inventory.setItem(9 * 4 + 2, tradeWithMoney ? addHundredMoney : abortButton);
            inventory.setItem(9 * 4 + 3, tradeWithMoney ? clearMoney : abortButton);
            inventory.setItem(9 * 5 + 0, readyButton);
            inventory.setItem(9 * 5 + 1, readyButton);
            inventory.setItem(9 * 5 + 2, abortButton);
            inventory.setItem(9 * 5 + 3, abortButton);
        }
        if (type == 1) {
            ItemStack waitingForPartner = design.getItem("readyAndWaitForPartner",
                    ChatColor.GREEN + Main.getInstance().getMessages().getString("ready_and_wait_for_partner"));
            for (int i = 0; i < 4; i++) {
                inventory.setItem(9 * 4 + i, waitingForPartner);
                inventory.setItem(9 * 5 + i, waitingForPartner);
            }
        }
        if (type == 2) {
            String[] acceptLore = Main.getInstance().getMessages().getString("button_accept_description").split("\n");
            for (int i = 0; i < acceptLore.length; i++) {
                acceptLore[i] = ChatColor.GRAY + acceptLore[i];
            }
            ItemStack acceptButton = design.getItem("acceptTrade",
                    ChatColor.DARK_GREEN + Main.getInstance().getMessages().getString("button_accept"), acceptLore);

            String[] abortLore = Main.getInstance().getMessages().getString("button_abort_description").split("\n");
            for (int i = 0; i < abortLore.length; i++) {
                abortLore[i] = ChatColor.GRAY + abortLore[i];
            }
            ItemStack abortButton = design.getItem("abortTrade",
                    ChatColor.RED + Main.getInstance().getMessages().getString("button_abort"), abortLore);

            inventory.setItem(9 * 4 + 0, acceptButton);
            inventory.setItem(9 * 4 + 1, acceptButton);
            inventory.setItem(9 * 4 + 2, abortButton);
            inventory.setItem(9 * 4 + 3, abortButton);
            inventory.setItem(9 * 5 + 0, acceptButton);
            inventory.setItem(9 * 5 + 1, acceptButton);
            inventory.setItem(9 * 5 + 2, abortButton);
            inventory.setItem(9 * 5 + 3, abortButton);
        }
        if (type == 3) {
            ItemStack waitingForPartner = design.getItem("acceptedAndWaitForPartner",
                    ChatColor.GREEN + Main.getInstance().getMessages().getString("accepted_and_wait_for_partner"));
            for (int i = 0; i < 4; i++) {
                inventory.setItem(9 * 4 + i, waitingForPartner);
                inventory.setItem(9 * 5 + i, waitingForPartner);
            }
        }
    }

}
