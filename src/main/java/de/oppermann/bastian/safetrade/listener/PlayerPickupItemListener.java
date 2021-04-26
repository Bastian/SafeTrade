package de.oppermann.bastian.safetrade.listener;

import de.oppermann.bastian.safetrade.Main;
import de.oppermann.bastian.safetrade.util.Trade;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.metadata.MetadataValue;

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

        // When a players trades items but then does not have enough place in their inventory, we drop the
        // items on the floor. To prevent other players from picking up these items, the item has a "item_owner"
        // metadata. The code below makes sure that the owner has 30 seconds to pick up the items before anyone
        // else is able to.
        if (event.getItem().hasMetadata("item_owner")) {
            String owner = "";
            long dropTimestamp = 0;
            for (MetadataValue metadata : event.getItem().getMetadata("item_owner")) {
                if (metadata.getOwningPlugin() == Main.getInstance()) {
                    owner = metadata.asString();
                }
            }
            for (MetadataValue metadata : event.getItem().getMetadata("drop_timestamp")) {
                if (metadata.getOwningPlugin() == Main.getInstance()) {
                    dropTimestamp = metadata.asLong();
                }
            }
            // If the item belongs to a player he has 30 seconds to pick it up before anyone else is allowed
            if (System.currentTimeMillis() - 1000 * 30 < dropTimestamp && !event.getPlayer().getName().equals(owner)) {
                event.setCancelled(true);
            }
        }
    }

}
