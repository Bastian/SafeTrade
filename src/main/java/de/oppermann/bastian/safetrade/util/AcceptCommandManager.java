package de.oppermann.bastian.safetrade.util;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import de.oppermann.bastian.safetrade.Main;

/**
 * This is a simple class for the <code>/trade accept</code> command.
 * <p>
 * (note: This class is copied from an old project of mine and should be rewritten...) 
 */
public class AcceptCommandManager {
    
    // the timeout is 30 seconds so we don't need to use UUIDs, do we?
	private final static HashMap<String, AcceptAction> actions = new HashMap<>();
	private final static HashMap<String, BukkitRunnable> runnables = new HashMap<>();
	
	private AcceptCommandManager() { /* nope */ }
	
	/**
	 * Gets the action that is waiting to be confirmed by the player.
	 * 
	 * @param player The player.
	 * @return The action that is waiting to be confirmed by the player, or <code>null</code> if there is no action.
	 */
	public static AcceptAction getAction(Player player) {
		if (actions.containsKey(player.getName().toLowerCase())) {
			return actions.get(player.getName().toLowerCase());
		} else {
			return null;
		}
	}
	
	/**
	 * This is called when the action is finished.
	 * You don't need to call this by yourself.
	 * 
	 * @param player The player.
	 * @param timeout <code>true</code> if the action was not performed.
	 */
	public static void finish(Player player, boolean timeout) {
		if (actions.containsKey(player.getName().toLowerCase())) {
			if (timeout) {
				actions.get(player.getName().toLowerCase()).onTimeout();
			}
			actions.remove(player.getName().toLowerCase());
		}
		
		if (runnables.containsKey(player.getName().toLowerCase())) {
			runnables.get(player.getName().toLowerCase()).cancel();
		}
	}
	
	/**
	 * Adds an action that replaces the old action if one exists.
	 * 
	 * @param player The player.
	 * @param action The (new) action.
	 */
	public static void addAction(Player player, AcceptAction action) {
		actions.put(player.getName().toLowerCase(), action);
		
		if (runnables.containsKey(player.getName().toLowerCase())) {
			runnables.get(player.getName().toLowerCase()).cancel();
		}
		
		final Player PLAYER = player;
		BukkitRunnable runnable = new BukkitRunnable() {			
			@Override
			public void run() {
				finish(PLAYER, true);
			}
		};
		
		runnable.runTaskLater(Main.getInstance(), 20 * 30);
		runnables.put(player.getName().toLowerCase(), runnable);
	}
	
}
