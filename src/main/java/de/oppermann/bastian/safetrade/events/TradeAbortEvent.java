package de.oppermann.bastian.safetrade.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is thrown when a trade gets aborted.
 */
public class TradeAbortEvent extends Event {

    // The handler list
    private static final HandlerList handlerList = new HandlerList();

    // The player who aborted the trade
    private final Player aborter;
    // The trading partner.
    private final Player tradingPartner;

    /**
     * Creates a new trade abort event.
     *
     * @param aborter        The player who aborted the trade.
     * @param tradingPartner The trading partner.
     */
    public TradeAbortEvent(Player aborter, Player tradingPartner) {
        this.aborter = aborter;
        this.tradingPartner = tradingPartner;
    }

    /**
     * Gets the player who aborted the trade.
     *
     * @return The player who aborted the trade.
     */
    public Player getAborter() {
        return aborter;
    }

    /**
     * Gets the trading partner.
     *
     * @return The trading partner.
     */
    public Player getTradingPartner() {
        return tradingPartner;
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