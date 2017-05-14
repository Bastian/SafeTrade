package de.oppermann.bastian.safetrade.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * This event is thrown when both players accept a trade.
 */
public class TradeSuccessEvent extends Event implements Cancellable {

    // The handler list
    private static final HandlerList handlerList = new HandlerList();

    // The two players
    private Player player1, player2;

    // The transferred items
    private List<ItemStack> itemsPlayer1To2, itemsPlayer2To1;
    // The transferred money
    private double money1To2, money2To1;

    // Defines if the event should be cancelled.
    private boolean cancelled;

    /**
     * Create a new trade success event.
     *
     * @param player1         The first player of the trade.
     * @param player2         The second player of the trade.
     * @param itemsPlayer1To2 The items which will be received by the second player.
     * @param itemsPlayer2To1 The items which will be received by the first player.
     * @param money1To2       The money which will be received by the second player.
     * @param money2To1       The money which will be received by the first player.
     */
    public TradeSuccessEvent(Player player1, Player player2, List<ItemStack> itemsPlayer1To2, List<ItemStack> itemsPlayer2To1, double money1To2, double money2To1) {
        this.player1 = player1;
        this.player2 = player2;
        this.itemsPlayer1To2 = itemsPlayer1To2;
        this.itemsPlayer2To1 = itemsPlayer2To1;
        this.money1To2 = money1To2;
        this.money2To1 = money2To1;
    }

    /**
     * Gets the first player of the trade.
     *
     * @return The first player of the trade.
     */
    public Player getPlayer1() {
        return player1;
    }

    /**
     * Gets the second player of the trade.
     *
     * @return The second player of the trade.
     */
    public Player getPlayer2() {
        return player2;
    }

    /**
     * Gets the items which will be received by the second player.
     *
     * @return The items which be received by the second player.
     */
    public List<ItemStack> getItemsPlayer1To2() {
        return itemsPlayer1To2;
    }

    /**
     * Gets the items which will be received by the first player.
     *
     * @return The items which will be received by the first player.
     */
    public List<ItemStack> getItemsPlayer2To1() {
        return itemsPlayer2To1;
    }

    /**
     * Gets the money which will be received by the second player.
     *
     * @return The money which be received by the second player.
     */
    public double getMoney1To2() {
        return money1To2;
    }

    /**
     * Gets the money which will be received by the second player.
     *
     * @return The money which be received by the second player.
     */
    public double getMoney2To1() {
        return money2To1;
    }

    /**
     * If the event is cancelled, the players won't be allowed to finish the trade.
     * Please notice both players <b>won't</b> get a message, if you cancel the event!
     *
     * @return Whether the event is cancelled or not.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * If the event is cancelled, the players won't be allowed to finish the trade.
     * Please notice both players <b>won't</b> get a message, if you cancel the event!
     *
     * @param cancel Whether the event should get cancelled or not.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    /**
     * Bukkit really loves this method, so I wanted to do it a flavour and add it. <3
     *
     * @return Just kidding. Custom events don't work without this method, that's the reason.
     */
    public static HandlerList getHandlerList() {
        return handlerList;
    }

}
