package de.oppermann.bastian.safetrade.util;

import org.bukkit.entity.Player;

/**
 * This interface contains all economy related methods the plugin needs.
 * If you're a developer and Vault doesn't support your economy plugin you can use this interface.
 */
public interface IEconomy {

    /**
     * Gets the amount of money the player owns.
     *
     * @param player The player.
     * @return The money the player owns.
     */
    double getMoney(Player player);

    /**
     * Withdraws the given amount from the player.
     *
     * @param player The player.
     * @param amount The amount of money to withdraw.
     */
    void withdrawMoney(Player player, double amount);

    /**
     * Deposits the given amount to the player.
     *
     * @param player The player.
     * @param amount The amount of money to deposit.
     */
    void depositMoney(Player player, double amount);

    /**
     * Formats the money into a nice readable format (e.g. "1 Dollar")
     *
     * @param amount The money to format.
     * @return The formated money.
     */
    String format(double amount);

}
