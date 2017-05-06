package de.oppermann.bastian.safetrade.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is thrown when a trade gets aborted.
 */
public class TradeAbortEvent extends Event {

    // The handler list
    private HandlerList handlerList = new HandlerList();

    // The player who aborted the trade
    private Player aborter;
    // The trading partner.
    private Player tradingPartner;

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

}