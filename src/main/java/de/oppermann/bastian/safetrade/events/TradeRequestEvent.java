package de.oppermann.bastian.safetrade.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is thrown when a player tries to request a trade.
 */
public class TradeRequestEvent extends Event implements Cancellable {

    // The handler list
    private static final HandlerList handlerList = new HandlerList();

    // The sender of the trade request.
    private final Player sender;
    // The target of the trade request.
    private final Player target;

    // Defines if the event should be cancelled.
    private boolean cancelled;

    /**
     * Creates a new trade request event.
     *
     * @param sender The sender of the trade request.
     * @param target The target of the trade request.
     */
    public TradeRequestEvent(Player sender, Player target) {
        this.sender = sender;
        this.target = target;
    }

    /**
     * Gets the player who sent the request.
     *
     * @return The player who sent the request.
     */
    public Player getSender() {
        return sender;
    }

    /**
     * Gets the player who received the trade request.
     *
     * @return The player who received the trade request.
     */
    public Player getTarget() {
        return target;
    }

    /**
     * If the event is cancelled, the sender won't be allowed to request a trade.
     * Please notice both players <b>won't</b> get a message, if you cancel the event!
     *
     * @return Whether the event is cancelled or not.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * If the event is cancelled, the sender won't be allowed to request a trade.
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