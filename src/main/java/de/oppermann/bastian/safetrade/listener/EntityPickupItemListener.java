package de.oppermann.bastian.safetrade.listener;

import de.oppermann.bastian.safetrade.Main;
import de.oppermann.bastian.safetrade.util.Trade;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.metadata.MetadataValue;

/**
 * This class prevents picking up items while trading.
 */
public class EntityPickupItemListener implements Listener {

    /**
     * This is called automatically by Bukkit.
     *
     * @param event The event.
     */
    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (Trade.getTradeOf(player) != null) {
            // players are not allowed to pick up items while trading
            event.setCancelled(true);
        }
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
            if (System.currentTimeMillis() - 1000 * 30 < dropTimestamp && !player.getName().equals(owner)) {
                event.setCancelled(true);
            }
        }
    }

}
