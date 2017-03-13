package de.oppermann.bastian.safetrade.listener;

import de.oppermann.bastian.safetrade.util.Trade;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Trade trade = Trade.getTradeOf(event.getEntity());
        if (trade == null) {
            return;
        }
        trade.abort(event.getEntity());
    }

}
