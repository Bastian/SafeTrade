package de.oppermann.bastian.safetrade.listener;

import de.oppermann.bastian.safetrade.Main;
import de.oppermann.bastian.safetrade.util.Trade;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;

/**
 * This class handles all drags in a inventory.
 */
public class InventoryDragListener implements Listener {

    /**
     * This is called automatically by Bukkit.
     *
     * @param event The event.
     */
    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getWhoClicked().getOpenInventory() == null) {
            return;
        }
        Trade trade = Trade.getTradeOf((Player) event.getWhoClicked());
        if (trade == null) {
            return;
        }

        List<Integer> allowedSlots = trade.getCurrentAllowedSlots(event.getWhoClicked().getUniqueId());
        for (int slot : event.getRawSlots()) {
            if (!allowedSlots.contains(slot)) {
                event.setCancelled(true);
                return;
            }
        }

        if (trade.isReadyOrHasAccepted(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        final Inventory partnerInventory = trade.getInventoryOfPartner(event.getWhoClicked().getUniqueId());

        for (Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            ItemStack toSet = entry.getValue().clone();
            partnerInventory.setItem(entry.getKey() + 5, toSet);
        }

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), new Runnable() {

            @Override
            public void run() {
                for (HumanEntity player : partnerInventory.getViewers()) {
                    ((Player) player).updateInventory();
                }
            }
        }, 1);
    }

}
