package de.oppermann.bastian.safetrade.listener;

import de.oppermann.bastian.safetrade.util.Trade;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

/**
 * This class prevents picking up items while trading.
 */
public class PlayerPickupItemListener implements Listener {

    /**
     * This is called automatically by Bukkit.
     *
     * @param event The event.
     */
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (Trade.getTradeOf(event.getPlayer()) != null) {
            // players are not allowed to pick up items while trading
            event.setCancelled(true);
        }
    }

}
