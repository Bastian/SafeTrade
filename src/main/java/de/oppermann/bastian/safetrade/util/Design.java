package de.oppermann.bastian.safetrade.util;

import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * This class is used to manage the trade inventory design.
 */
public class Design {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public Design(JavaPlugin plugin) {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "design.yml");
        if (!configFile.exists()) {
            try {
                FileUtils.copy(plugin.getResource("design.yml"), new File(plugin.getDataFolder(), "design.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Creates an {@link ItemStack}.
     *
     * @param id          The id of the item (e.g. 'acceptTrade')
     * @param displayName The display name of the ItemStack.
     * @param lore        The lore of the ItemStack.
     * @return A ItemStack with the given values.
     */
    public ItemStack getItem(String id, String displayName, String... lore) {
        String item = config.getString(id);
        if (item == null) {
            return new ItemStack(Material.TNT);
        }
        String[] idAndData = item.split(":");
        int itemId = 0;
        byte itemData = 0;
        if (idAndData.length == 0) {
            return new ItemStack(Material.TNT);
        }

        // Get the id as integer
        if (idAndData.length >= 1) {
            try {
                itemId = Integer.parseInt(idAndData[0]);
            } catch (NumberFormatException e) {
                return new ItemStack(Material.TNT);
            }
        }

        // Get the data as byte
        if (idAndData.length >= 2) {
            try {
                itemData = Byte.parseByte(idAndData[1]);
            } catch (NumberFormatException e) {
                return new ItemStack(Material.TNT);
            }
        }

        ItemStack itemStack = new ItemStack(itemId, 1, itemData);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        if (lore.length != 0) {
            itemMeta.setLore(Lists.newArrayList(lore));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    /**
     * Reloads the design.yml file.
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "design.yml"));
        config.options().copyDefaults(true);
    }

}
