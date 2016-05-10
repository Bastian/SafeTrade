package de.oppermann.bastian.safetrade;

import de.oppermann.bastian.safetrade.commands.TradeCommand;
import de.oppermann.bastian.safetrade.commands.TradeTabCompleter;
import de.oppermann.bastian.safetrade.listener.InventoryClickListener;
import de.oppermann.bastian.safetrade.listener.InventoryCloseListener;
import de.oppermann.bastian.safetrade.listener.InventoryDragListener;
import de.oppermann.bastian.safetrade.listener.PlayerInteractEntityListener;
import de.oppermann.bastian.safetrade.util.FileUtils;
import de.oppermann.bastian.safetrade.util.IEconomy;
import de.oppermann.bastian.safetrade.util.ResourceBundleControl;
import de.oppermann.bastian.safetrade.util.Trade;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * The main class of this plugin.
 */
public class Main extends JavaPlugin {

    /**
     * The initialized instance if this class.
     */
    private static Main instance = null;

    /**
     * The {@link ResourceBundle} which contains all messages.
     */
    private ResourceBundle messages;

    /**
     * The economy.
     */
    private IEconomy economy = null;

    /**
     * Whether trading with money should be allowed or not.
     */
    private boolean tradeWithMoney = false;

    /*
     * (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        instance = this;

        loadConfiguration();

        getCommand("trade").setExecutor(new TradeCommand());
        getCommand("trade").setTabCompleter(new TradeTabCompleter());

        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryCloseListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryDragListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);

        super.onEnable();
    }

    /*
     * (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        for (Trade trade : Trade.getActiveTrades()) { // abort all trades
            trade.abort(null);
        }
        super.onDisable();
    }

    /**
     * Gets the {@link Economy} of the server.
     *
     * @return The economy of the server.
     */
    private Economy getVaultEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            return economyProvider.getProvider();
        }
        return null;
    }

    /**
     * Loads the configuration.
     */
    private void loadConfiguration() {
        saveDefaultConfig();
        String strLocale = getConfig().getString("language");
        Locale locale;
        if (strLocale == null || strLocale.equals("auto")) {
            locale = Locale.getDefault();
        } else {
            locale = Locale.forLanguageTag(strLocale);
        }
        String encoding = getConfig().getString("encoding", "UTF-8");
        messages = loadLanguage(locale, encoding); // load the language file

        tradeWithMoney = getConfig().getBoolean("tradeWithMoney", true);
        if (tradeWithMoney) {
            try {
                final Economy economy = getVaultEconomy();
                if (economy == null) {
                    getLogger().log(Level.WARNING, "Cannot find any economy plugin!");
                } else {
                    getLogger().log(Level.INFO, "Using " + economy.getName());
                    setIEconomy(new IEconomy() {

                        @Override
                        public void withdrawMoney(Player player, double amount) {
                            economy.withdrawPlayer(player, amount);
                        }

                        @Override
                        public double getMoney(Player player) {
                            return economy.getBalance(player);
                        }

                        @Override
                        public void depositMoney(Player player, double amount) {
                            economy.depositPlayer(player, amount);
                        }

                        @Override
                        public String format(double amount) {
                            return economy.format(amount);
                        }
                    });
                }
            } catch (NoClassDefFoundError e) {
                getLogger().log(Level.WARNING, "Cannot find Vault!");
            }
        }
    }

    /**
     * Loads the {@link ResourceBundle} for the given {@link Locale locale}.
     *
     * @param locale   The locale.
     * @param encoding The encoding.
     * @return The ResourceBundle. If no ResourceBundle for the given locale was found it will return the default one.
     */
    private ResourceBundle loadLanguage(Locale locale, String encoding) {
        File languageFolder = new File(getDataFolder(), "languages");
        languageFolder.mkdirs();

        try { // copy the default messages to file
            FileUtils.copy(getResource("Messages.properties"), new File(languageFolder, "Messages.properties"));
            // en = default
            FileUtils.copy(getResource("Messages.properties"), new File(languageFolder, "Messages_en.properties"));
            FileUtils.copy(getResource("Messages_de.properties"), new File(languageFolder, "Messages_de.properties"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not copy language resources to " + languageFolder.getPath(), e);
        }

        ClassLoader classLoader = getClassLoader(); // ResourceBundle requires a class to load the .properties-files
        try {
            classLoader = new URLClassLoader(new URL[]{languageFolder.toURI().toURL()});
        } catch (MalformedURLException e) {
            getLogger().log(Level.SEVERE, "Could not load messages from file. Using default ones.", e);
        }

        ResourceBundle messages;
        try {
            messages = ResourceBundle.getBundle("Messages", locale, classLoader, new ResourceBundleControl(encoding));
        } catch (MissingResourceException e) {
            getLogger().log(Level.WARNING, "Could not find messages for locale "
                    + locale.toString() + "! Using the default locale now!");
            messages = ResourceBundle.getBundle("Messages");
        }
        return messages;
    }

    /**
     * Reloads all configuration files.
     */
    public void reload() {
        this.reloadConfig();
        // TODO reload .properties files.
    }

    /**
     * Sets the economy.
     *
     * @param economy The economy.
     */
    public void setIEconomy(IEconomy economy) {
        if (tradeWithMoney) {
            this.economy = economy;
        }
    }

    /**
     * Gets the economy.
     *
     * @return The economy
     */
    public IEconomy getEconomy() {
        return this.economy;
    }

    /**
     * Gets the {@link ResourceBundle} which contains all messages.
     *
     * @return The ResourceBundle which contains all messages.
     */
    public ResourceBundle getMessages() {
        return messages;
    }

    /**
     * Gets the initialized instance if this class.
     *
     * @return The initialized instance if this class.
     */
    public static Main getInstance() {
        return instance;
    }

}
