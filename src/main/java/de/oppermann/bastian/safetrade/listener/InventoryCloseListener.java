package de.oppermann.bastian.safetrade.listener;

import java.util.ResourceBundle;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import de.oppermann.bastian.safetrade.Main;
import de.oppermann.bastian.safetrade.util.Trade;

public class InventoryCloseListener implements Listener {
    
    /**
     * The {@link ResourceBundle} which contains all messages.
     */
    private ResourceBundle messages = Main.getInstance().getMessages();
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = messages.getString("tradinginventory_title");
        title = title.length() > 32 ? title.substring(0, 32) : title;
        if (event.getInventory().getName().equals(title)) {
            Trade trade = Trade.getTradeOf((Player) event.getPlayer());
            if (trade == null) {
                return;
            }
            trade.abort((Player) event.getPlayer());
        }
    }

}
