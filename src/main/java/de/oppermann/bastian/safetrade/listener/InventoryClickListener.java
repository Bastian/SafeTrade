package de.oppermann.bastian.safetrade.listener;

import de.oppermann.bastian.safetrade.Main;
import de.oppermann.bastian.safetrade.util.Trade;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ResourceBundle;

/**
 * This class handles all clicks in a inventory.
 */
public class InventoryClickListener implements Listener {

    /**
     * This is called automatically by bukkit.
     *
     * @param event The event.
     */
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (getClickedInventory(event) == null) {
            return;
        }
        Trade trade = Trade.getTradeOf((Player) event.getWhoClicked());
        if (trade == null) {
            return;
        }
        if (getClickedInventory(event) == event.getWhoClicked().getOpenInventory().getTopInventory()) {
            final Inventory partnerInventory = trade.getInventoryOfPartner(event.getWhoClicked().getUniqueId());
            if (trade.getCurrentAllowedSlots(event.getWhoClicked().getUniqueId()).contains(event.getRawSlot())) {
                if (trade.isReadyOrHasAccepted(event.getWhoClicked().getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
                boolean checkedAction = false;
                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    partnerInventory.setItem(event.getRawSlot() + 5, null);
                    checkedAction = true;
                }
                if (event.getAction() == InventoryAction.PICKUP_ONE) {
                    ItemStack toSet = getClickedInventory(event).getItem(event.getRawSlot()).clone();
                    toSet.setAmount(toSet.getAmount() - 1);
                    if (toSet.getAmount() == 0) {
                        toSet = null;
                    }
                    partnerInventory.setItem(event.getRawSlot() + 5, toSet);
                    checkedAction = true;
                }
                if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    ItemStack toSet = getClickedInventory(event).getItem(event.getRawSlot()).clone();
                    toSet.setAmount(toSet.getAmount() - (int) ((double) toSet.getAmount() / (double) 2 + 0.5));
                    if (toSet.getAmount() == 0) {
                        toSet = null;
                    }
                    partnerInventory.setItem(event.getRawSlot() + 5, toSet);
                    checkedAction = true;
                }
                if (event.getAction() == InventoryAction.DROP_ONE_SLOT) {
                    ItemStack current = getClickedInventory(event).getItem(event.getRawSlot());
                    ItemStack toSet = current.clone();
                    toSet.setAmount(toSet.getAmount() - 1);
                    if (toSet.getAmount() == 0) {
                        toSet = null;
                    }
                    partnerInventory.setItem(event.getRawSlot() + 5, toSet);
                    checkedAction = true;
                }
                if (event.getAction() == InventoryAction.PLACE_ONE) {
                    ItemStack current = getClickedInventory(event).getItem(event.getRawSlot());
                    ItemStack toSet = event.getCursor().clone();
                    if (current != null && current.isSimilar(toSet)) {
                        toSet.setAmount(current.getAmount() + 1);
                    } else {
                        toSet.setAmount(1);
                    }
                    partnerInventory.setItem(event.getRawSlot() + 5, toSet);
                    checkedAction = true;
                }
                if (event.getAction() == InventoryAction.PLACE_ALL) {
                    ItemStack current = getClickedInventory(event).getItem(event.getRawSlot());
                    ItemStack toSet = event.getCursor().clone();
                    if (current != null && current.isSimilar(toSet)) {
                        toSet.setAmount(current.getAmount() + toSet.getAmount());
                    }
                    partnerInventory.setItem(event.getRawSlot() + 5, toSet);
                    checkedAction = true;
                }
                if (event.getAction() == InventoryAction.PLACE_SOME) {
                    ItemStack current = getClickedInventory(event).getItem(event.getRawSlot());
                    ItemStack toSet = event.getCursor().clone();
                    if (current != null && current.isSimilar(toSet)) {
                        toSet.setAmount(current.getAmount() + toSet.getAmount());
                    }
                    if (toSet.getAmount() > toSet.getMaxStackSize()) {
                        toSet.setAmount(toSet.getMaxStackSize());
                    }
                    partnerInventory.setItem(event.getRawSlot() + 5, toSet);
                    checkedAction = true;
                }
                if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
                    ItemStack toSet = event.getCursor().clone();
                    partnerInventory.setItem(event.getRawSlot() + 5, toSet);
                    checkedAction = true;
                }
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    partnerInventory.setItem(event.getRawSlot() + 5, null);
                    checkedAction = true;
                }
                if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                    event.setCancelled(true);
                    return;
                }
                if (!checkedAction) {
                    event.setCancelled(true);
                    return;
                }

                Bukkit.getScheduler().runTaskLater(Main.getInstance(), new Runnable() {

                    @Override
                    public void run() {
                        for (HumanEntity player : partnerInventory.getViewers()) {
                            ((Player) player).updateInventory();
                        }
                    }
                }, 1);
            } else { // if they aren't the allowed slots
                if (trade.getCurrentAcceptSlots(event.getWhoClicked().getUniqueId()).contains(event.getRawSlot())) {
                    trade.approve((Player) event.getWhoClicked());
                } else if (trade.getCurrentAbortSlots(event.getWhoClicked().getUniqueId()).contains(event.getRawSlot())) {
                    trade.abort((Player) event.getWhoClicked());
                }

                for (byte i = 0; i < 4; i++) {
                    if (trade.getCurrentIncreaseMoneySlot(event.getWhoClicked().getUniqueId(), i).contains(event.getRawSlot())) {
                        trade.changeMoney(event.getWhoClicked().getUniqueId(), i, event.isLeftClick());
                    }
                }
                event.setCancelled(true);
            }
        } else {
            if (event.getAction() != InventoryAction.PICKUP_ALL &&
                    event.getAction() != InventoryAction.PICKUP_HALF &&
                    event.getAction() != InventoryAction.PICKUP_SOME &&
                    event.getAction() != InventoryAction.PICKUP_ONE &&
                    event.getAction() != InventoryAction.PLACE_ALL &&
                    event.getAction() != InventoryAction.PLACE_SOME &&
                    event.getAction() != InventoryAction.PLACE_ONE &&
                    event.getAction() != InventoryAction.DROP_ONE_SLOT &&
                    event.getAction() != InventoryAction.SWAP_WITH_CURSOR) {
                event.setCancelled(true);
            }

            if (getClickedInventory(event) != null &&
                Main.getInstance().getBlacklist().isBlacklisted(event.getCurrentItem()))
            {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Because Bukkit (instead of Spigot) does not has an InventoryClickEvent#getClickedInventory() method
     * we have to create it.
     *
     * @param event The InventoryClickEvent.
     * @return The clicked inventory. <code>null</code> if no inventory was clicked.
     */
    private Inventory getClickedInventory(InventoryClickEvent event) {
        InventoryView view = event.getView();
        // Check out the spigot repo, if you want to see the original spigot method:
        // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/spigot/browse/Bukkit-Patches/0009-InventoryClickEvent-getClickedInventory.patch
        if (event.getRawSlot() < 0) {
            return null;
        } else if (view.getTopInventory() != null && event.getRawSlot() < view.getTopInventory().getSize()) {
            return view.getTopInventory();
        } else {
            return view.getBottomInventory();
        }
    }

}
