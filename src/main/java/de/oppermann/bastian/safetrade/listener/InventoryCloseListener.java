package de.oppermann.bastian.safetrade.listener;

import de.oppermann.bastian.safetrade.Main;
import de.oppermann.bastian.safetrade.util.Trade;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.ResourceBundle;

/**
 * This listener aborts a trade if a player closes his inventory.
 */
public class InventoryCloseListener implements Listener {

    /**
     * This is called automatically by bukkit.
     *
     * @param event The event.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Trade trade = Trade.getTradeOf((Player) event.getPlayer());
        if (trade == null) {
            return;
        }
        trade.abort((Player) event.getPlayer());
    }

}
