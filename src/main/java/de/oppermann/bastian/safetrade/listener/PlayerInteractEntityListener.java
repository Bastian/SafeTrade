package de.oppermann.bastian.safetrade.listener;

import de.oppermann.bastian.safetrade.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * This class is used for "fast trade".
 */
public class PlayerInteractEntityListener implements Listener {

    /**
     * This is called automatically by Bukkit.
     *
     * @param event The event.
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        try {
            if (!event.getHand().equals(org.bukkit.inventory.EquipmentSlot.HAND)) {
                return;
            }
        } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
            // Method does not exist in 1.8 or below, that's the reason for the try-catch
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }
        if (!Main.getInstance().getConfig().getBoolean("fastTrade")) {
            return;
        }
        Player target = (Player) event.getRightClicked();
        player.chat("/trade " + target.getName()); // this is the easiest way :)
    }

}
