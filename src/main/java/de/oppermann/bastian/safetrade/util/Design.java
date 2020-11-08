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
import java.util.logging.Level;

/**
 * This class is used to manage the trade inventory design.
 */
public class Design {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration fallbackConfig;

    /**
     * Creates a new Design object.
     *
     * @param plugin The SafeTrade plugin.
     */
    public Design(JavaPlugin plugin) {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "style.yml");
        if (!configFile.exists()) {
            try {
                FileUtils.copy(plugin.getResource("style.yml"), new File(plugin.getDataFolder(), "style.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            FileUtils.copy(plugin.getResource("style.yml"), new File(plugin.getDataFolder(), "style_original.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        File fallbackConfigFile = new File(plugin.getDataFolder(), "style_original.yml");

        config = YamlConfiguration.loadConfiguration(configFile);
        fallbackConfig = YamlConfiguration.loadConfiguration(fallbackConfigFile);
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
            item = fallbackConfig.getString(id);
            if (item == null) {
                plugin.getLogger().log(Level.SEVERE, "Failed to find material for id '" + id + "'! " +
                        "Please update to the latest SafeTrade version or report this as a bug if you are already " +
                        "using the latest version!");
                return new ItemStack(Material.TNT);
            }
        }

        Material material = Material.matchMaterial(item);
        if (material == null) {
            String invalidItem = item;
            item = fallbackConfig.getString(id);
            material = Material.matchMaterial(item);
            if (material == null) {
                material = Material.TNT;
                lore = new String[] { "Invalid material in style.yml file for " + id + "!\n" +
                        "Please update to the latest SafeTrade version or report this as a bug if you are already " +
                        "using the latest version!" };
            } else {
                plugin.getLogger().warning("Your style.yml file contains an invalid material for id '" + id +
                        "': '" + invalidItem + "'. Please fix your configuration. Take a look at the " +
                        "style_original.yml file for a correct configuration.");
            }
        }

        ItemStack itemStack = new ItemStack(material, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        if (lore.length != 0) {
            itemMeta.setLore(Lists.newArrayList(lore));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    /**
     * Reloads the style.yml file.
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "style.yml"));
        config.options().copyDefaults(true);
    }

}
