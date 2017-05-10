package de.oppermann.bastian.safetrade.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This class is used to manage the blacklist.
 */
public class Blacklist {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    /**
     * Creates a new Blacklist object.
     *
     * @param plugin The SafeTrade plugin.
     */
    public Blacklist(JavaPlugin plugin) {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "blacklist.yml");
        if (!configFile.exists()) {
            try {
                FileUtils.copy(plugin.getResource("blacklist.yml"), new File(plugin.getDataFolder(), "blacklist.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Checks if the blacklist is enabled.
     *
     * @return Whether the blacklist is enabled or not.
     */
    public boolean isEnabled() {
        return config.getBoolean("enabled", false);
    }

    /**
     * Checks if a given item is blacklisted.
     *
     * @param itemToCheck The item to check.
     * @return Whether the item is blacklisted or not. Always <code>false</code> if the blacklist is disabled.
     */
    public boolean isBlacklisted(ItemStack itemToCheck) {
        if (!isEnabled() || itemToCheck == null) {
            // Always return false if blacklist is disabled
            return false;
        }
        List<String> blacklistedItems = config.getStringList("blacklist");
        for (String item : blacklistedItems) {

            String[] split = item.split(":");
            String itemId = split[0];
            String itemData = split.length > 1 ? split[1] : null;

            if (itemData == null) {
                // Only check the item id
                if (String.valueOf(itemToCheck.getType().getId()).equals(itemId)) {
                    return true;
                }
            } else {
                // Check item id and data
                if (String.valueOf(itemToCheck.getType().getId()).equals(itemId) &&
                    String.valueOf(itemToCheck.getData().getData()).equals(itemData))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reloads the blacklist.yml file.
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "blacklist.yml"));
        config.options().copyDefaults(true);
    }

}
