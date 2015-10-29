package de.oppermann.bastian.safetrade.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import de.oppermann.bastian.safetrade.Main;

/**
 * This class is used to send json stings.
 * <p>(You need json to use all this fancy stuff like clicking on command)
 */
public class JSONUtil {
	
	private static boolean nmsFailed = false;
	
	private static Class<?> classCraftPlayer;
	private static Class<?> classPacketPlayOutChat;
	private static Class<?> classChatSerializer;
	private static Class<?> classIChatBaseComponent;
	private static Class<?> classPacket;
	
	private static String version; // the craftbukkit version

	static {
		String path = Bukkit.getServer().getClass().getPackage().getName();
		version = path.substring(path.lastIndexOf(".") + 1, path.length());
		
		try {
			classCraftPlayer = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
			classPacketPlayOutChat = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
			classIChatBaseComponent = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
			classPacket = Class.forName("net.minecraft.server." + version + ".Packet");
		} catch (ClassNotFoundException e) {
			// incompatible version
			Main.getInstance().getLogger().log(Level.WARNING, "Could not access NMS classes. Please use a plugin version which is compatible with your server version for full functionality.", e);
			nmsFailed = true;
		}
		
		if (!nmsFailed) {
    		try {
                classChatSerializer = Class.forName("net.minecraft.server." + version + ".ChatSerializer");
            } catch (ClassNotFoundException e) { // if the first one failed the server version is younger than 1.8.3
                for (Class<?> clazz : classIChatBaseComponent.getDeclaredClasses()) {
                    if (clazz.getSimpleName().equals("ChatSerializer")) {
                        classChatSerializer = clazz;
                        break;
                    }
                }
                if (classChatSerializer == null) {
                    Main.getInstance().getLogger().log(Level.SEVERE, "Could not access ChatSerializer NMS class. Please use a plugin version which is compatible with your server version for full functionality.");
                    e.printStackTrace();
                    nmsFailed = true;
                }
            }
		}

	}
	
	private JSONUtil() { /* nope */ }
	
	/**
	 * Sends a raw json string to the player.
	 * 
	 * @param player The player.
	 * @param json The json string.
	 * @return Whether it succeeded or failed.
	 */
	public static boolean sendJSONText(Player player, String json) {			
		if (!nmsFailed) {
			try {
				Object entityPlayer = classCraftPlayer.getMethod("getHandle").invoke(player);
				Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);					
				Method sendPacketMethod = playerConnection.getClass().getMethod("sendPacket", classPacket);
				Object iChatBaseComponent = classChatSerializer.getMethod("a", String.class).invoke(playerConnection, json);
				Object packetPlayOutChat = classPacketPlayOutChat.getConstructor(classIChatBaseComponent).newInstance(iChatBaseComponent);
				
				// send packet to the player
				sendPacketMethod.invoke(playerConnection, packetPlayOutChat);
				return true;
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException | IllegalArgumentException | NoSuchFieldException | InstantiationException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}

}
