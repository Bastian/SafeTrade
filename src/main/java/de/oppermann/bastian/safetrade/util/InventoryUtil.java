package de.oppermann.bastian.safetrade.util;

import com.google.common.collect.Lists;
import de.oppermann.bastian.safetrade.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * This class includes some useful function related to trade inventories.
 */
public class InventoryUtil {

    /**
     * The {@link ResourceBundle} which contains all messages.
     */
    private static ResourceBundle messages = Main.getInstance().getMessages();

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
     * Creates an {@link ItemStack}.
     *
     * @param type         The type of the ItemStack.
     * @param data         The data of the ItemStack.
     * @param disyplayName The display name of the ItemStack.
     * @param lore         The lore of the ItemStack.
     * @return A ItemStack with the given values.
     */
    public static ItemStack createItemStack(Material type, byte data, String disyplayName, String... lore) {
        ItemStack itemStack = new ItemStack(type, 1, data);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(disyplayName);
        if (lore.length != 0) {
            itemMeta.setLore(Lists.newArrayList(lore));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    /**
     * Creates a new trading inventory.
     *
     * @return A new trading inventory.
     */
    public static Inventory createInventory() {
        return createInventory(false);
    }

    /**
     * Creates a new trading inventory.
     *
     * @param tradeWithMoney Should it be allowed to trade with money?
     * @return A new trading inventory.
     */
    public static Inventory createInventory(boolean tradeWithMoney) {
        String title = messages.getString("tradinginventory_title");
        title = title.length() > 32 ? title.substring(0, 32) : title;
        Inventory defaultTradeInventory = Bukkit.createInventory(null, 9 * 6, title);

        ItemStack seperateStack = createItemStack(Material.IRON_FENCE, (byte) 0, ChatColor.GOLD.toString());

        for (int i = 0; i < 6; i++) {
            defaultTradeInventory.setItem(9 * i + 4, seperateStack.clone());
        }
        for (int i = 0; i < 9; i++) {
            defaultTradeInventory.setItem(9 * 3 + i, seperateStack.clone());
        }

        setPartnerStatus(defaultTradeInventory, false, ChatColor.RED + messages.getString("partner_not_ready"));


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
    public static void synchronize(Inventory copyFrom, Inventory copyTo, boolean tradeWithMoney) {
        for (int slot : tradeWithMoney ? TRADING_SLOTS_LEFT_WITH_MONEY : TRADING_SLOTS_LEFT_WITHOUT_MONEY) {
            copyTo.setItem(slot + 5, copyFrom.getItem(slot));
        }
    }

    /**
     * Sets the status of the partner.
     *
     * @param inventory The inventory to modify.
     * @param status    The status (<code>true</code> = ready).
     * @param text      The text of the status item.
     */
    public static void setPartnerStatus(Inventory inventory, boolean status, String text) {
        ItemStack partnerStatus = createItemStack(
                Material.INK_SACK, status ? (byte) 10 : (byte) 8, text);
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
    public static void setMoney(Inventory inventory, int money, boolean leftSide) {
        int slot = leftSide ? 9 * 2 + 3 : 9 * 2 + 8;

        Material type = Material.DIAMOND_BLOCK;
        if (money < 10000) {
            type = Material.DIAMOND;
        }
        if (money < 1000) {
            type = Material.GOLD_BLOCK;
        }
        if (money < 100) {
            type = Material.GOLD_INGOT;
        }
        if (money < 10) {
            type = Material.GOLD_NUGGET;
        }
        if (money <= 0) {
            type = Material.BARRIER;
        }
        String strMoney = String.valueOf(money);
        if (Main.getInstance().getEconomy() != null) {
            strMoney = Main.getInstance().getEconomy().format(money);
        }
        ItemStack itemStack = createItemStack(type, (byte) 0, messages.getString("offered_money").replace("{money}", strMoney));
        if (type != Material.BARRIER) {
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
    @SuppressWarnings("deprecation") // screw you Mojang!
    public static void setOwnControlField(Inventory inventory, byte type, boolean tradeWithMoney) {
        if (type == 0) {
            String[] readyLore = messages.getString("button_ready_description").split("\n");
            for (int i = 0; i < readyLore.length; i++) {
                readyLore[i] = ChatColor.GRAY + readyLore[i];
            }
            ItemStack readyButton = createItemStack(Material.WOOL, (byte) DyeColor.LIME.getData(),
                    ChatColor.DARK_GREEN + messages.getString("button_ready"), readyLore);

            String[] abortLore = messages.getString("button_abort_description").split("\n");
            for (int i = 0; i < abortLore.length; i++) {
                abortLore[i] = ChatColor.GRAY + abortLore[i];
            }
            ItemStack abortButton = createItemStack(Material.WOOL, DyeColor.RED.getData(),
                    ChatColor.RED + messages.getString("button_abort"), abortLore);

            String[] addMoneyLore = messages.getString("button_add_money_description").split("\n");
            for (int i = 0; i < addMoneyLore.length; i++) {
                addMoneyLore[i] = ChatColor.GRAY + addMoneyLore[i];
            }

            String strMoney = "1";
            if (Main.getInstance().getEconomy() != null) {
                strMoney = Main.getInstance().getEconomy().format(1);
            }

            ItemStack addOneMoney = createItemStack(Material.GOLD_NUGGET, (byte) 0,
                    ChatColor.GOLD + messages.getString("button_add_money").replace("{money}", strMoney), addMoneyLore);

            strMoney = "10";
            if (Main.getInstance().getEconomy() != null) {
                strMoney = Main.getInstance().getEconomy().format(10);
            }
            ItemStack addTenMoney = createItemStack(Material.GOLD_INGOT, (byte) 0,
                    ChatColor.GOLD + messages.getString("button_add_money").replace("{money}", strMoney), addMoneyLore);

            strMoney = "100";
            if (Main.getInstance().getEconomy() != null) {
                strMoney = Main.getInstance().getEconomy().format(100);
            }
            ItemStack addHundredMoney = createItemStack(Material.GOLD_BLOCK, (byte) 0,
                    ChatColor.GOLD + messages.getString("button_add_money").replace("{money}", strMoney), addMoneyLore);

            String[] clearMoneyLore = messages.getString("button_clear_money_description").split("\n");
            for (int i = 0; i < clearMoneyLore.length; i++) {
                clearMoneyLore[i] = ChatColor.GRAY + clearMoneyLore[i];
            }
            ItemStack clearMoney = createItemStack(Material.BARRIER, (byte) 0,
                    ChatColor.RED + messages.getString("button_clear_money"), clearMoneyLore);

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
            ItemStack waitingForPartner = createItemStack(Material.STAINED_GLASS_PANE, DyeColor.LIME.getData(),
                    ChatColor.GREEN + messages.getString("ready_and_wait_for_partner"));
            for (int i = 0; i < 4; i++) {
                inventory.setItem(9 * 4 + i, waitingForPartner);
                inventory.setItem(9 * 5 + i, waitingForPartner);
            }
        }
        if (type == 2) {
            String[] acceptLore = messages.getString("button_accept_description").split("\n");
            for (int i = 0; i < acceptLore.length; i++) {
                acceptLore[i] = ChatColor.GRAY + acceptLore[i];
            }
            ItemStack acceptButton = createItemStack(Material.WOOL, (byte) DyeColor.LIME.getData(),
                    ChatColor.DARK_GREEN + messages.getString("button_accept"), acceptLore);

            String[] abortLore = messages.getString("button_abort_description").split("\n");
            for (int i = 0; i < abortLore.length; i++) {
                abortLore[i] = ChatColor.GRAY + abortLore[i];
            }
            ItemStack abortButton = createItemStack(Material.WOOL, DyeColor.RED.getData(),
                    ChatColor.RED + messages.getString("button_abort"), abortLore);

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
            ItemStack waitingForPartner = createItemStack(Material.STAINED_GLASS_PANE, DyeColor.LIME.getData(),
                    ChatColor.GREEN + messages.getString("accepted_and_wait_for_partner"));
            for (int i = 0; i < 4; i++) {
                inventory.setItem(9 * 4 + i, waitingForPartner);
                inventory.setItem(9 * 5 + i, waitingForPartner);
            }
        }
    }

}
