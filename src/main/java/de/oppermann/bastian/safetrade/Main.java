package de.oppermann.bastian.safetrade;

import de.oppermann.bastian.safetrade.commands.TradeCommand;
import de.oppermann.bastian.safetrade.commands.TradeTabCompleter;
import de.oppermann.bastian.safetrade.listener.InventoryClickListener;
import de.oppermann.bastian.safetrade.listener.InventoryCloseListener;
import de.oppermann.bastian.safetrade.listener.InventoryDragListener;
import de.oppermann.bastian.safetrade.listener.PlayerDeathListener;
import de.oppermann.bastian.safetrade.listener.PlayerInteractEntityListener;
import de.oppermann.bastian.safetrade.listener.PlayerPickupItemListener;
import de.oppermann.bastian.safetrade.util.Blacklist;
import de.oppermann.bastian.safetrade.util.Design;
import de.oppermann.bastian.safetrade.util.FileUtils;
import de.oppermann.bastian.safetrade.util.IEconomy;
import de.oppermann.bastian.safetrade.util.InventoryUtil;
import de.oppermann.bastian.safetrade.util.ResourceBundleControl;
import de.oppermann.bastian.safetrade.util.Trade;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
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
     * The design of the trading inventory.
     */
    private Design design;

    /**
     * The item blacklist.
     */
    private Blacklist blacklist;

    /**
     * The inventory util.
     */
    private InventoryUtil inventoryUtil;

    /**
     * The economy.
     */
    private IEconomy economy = null;

    /**
     * Whether trading with money should be allowed or not.
     */
    private boolean tradeWithMoney = false;

    /**
     * Successful trades since last submit.
     */
    private int successfulTrades = 0;

    /**
     * Aborted trades since last submit.
     */
    private int abortedTrades = 0;

    /**
     * Whether the server has vault or not.
     */
    private boolean hasVault = false;

    /**
     * The name of the used economy plugin.
     */
    private String economyName = "No Vault installed";

    /*
     * (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        instance = this;

        loadConfiguration();

        Objects.requireNonNull(getCommand("trade")).setExecutor(new TradeCommand());
        Objects.requireNonNull(getCommand("trade")).setTabCompleter(new TradeTabCompleter());

        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryCloseListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryDragListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerPickupItemListener(), this);

        // start metrics
        setupCharts(new Metrics(this, 4));
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
    }

    /**
     * Setups the custom starts for bStats.
     *
     * @param metrics The metrics class.
     */
    private void setupCharts(Metrics metrics) {
        // language in config
        metrics.addCustomChart(new SimplePie("used_language", () ->
                getConfig().getString("language", "auto")));

        metrics.addCustomChart(new SimplePie("default_locale", () ->
                Locale.getDefault().toLanguageTag()));

        // encoding in config
        metrics.addCustomChart(new SimplePie("encoding", () ->
                getConfig().getString("encoding", "UTF-8")));

        // tradeWithMoney in config
        metrics.addCustomChart(new SimplePie("money_enabled", () ->
                getConfig().getBoolean("tradeWithMoney", true) ? "enabled" : "disabled"));

        // noDebts in config
        metrics.addCustomChart(new SimplePie("no_debts_enabled", () ->
                getConfig().getBoolean("noDebts", true) ? "enabled" : "disabled"));

        // maxTradingDistance in config
        metrics.addCustomChart(new SimplePie("max_trading_distance", () ->
                String.valueOf(getConfig().getInt("maxTradingDistance", 15))));

        // tradeThroughWorlds in config
        metrics.addCustomChart(new SimplePie("trade_through_worlds_enabled", () ->
                getConfig().getBoolean("tradeThroughWorlds", false) ? "enabled" : "disabled"));

        // fastTrade in config
        metrics.addCustomChart(new SimplePie("fast_trade_enabled", () ->
                getConfig().getBoolean("fastTrade", false) ? "enabled" : "disabled"));

        // successful trades
        metrics.addCustomChart(new SingleLineChart("successful_trades", () -> successfulTrades));

        // aborted trades
        metrics.addCustomChart(new SingleLineChart("aborted_trades", () -> abortedTrades));

        // Is Vault used?
        metrics.addCustomChart(new SimplePie("vault_used", () -> hasVault ? "Used" : "Not used"));

        // The used economy plugin
        metrics.addCustomChart(new SimplePie("economy_plugin", () -> economyName));

        // Download source
        metrics.addCustomChart(new SimplePie("download_source", () -> {
            // Now the only source.
            return "spigotmc.org";
        }));
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
        if (design == null) {
            design = new Design(this);
            inventoryUtil = new InventoryUtil(design);
        } else {
            design.reload();
        }
        if (blacklist == null) {
            blacklist = new Blacklist(this);
        } else {
            blacklist.reload();
        }
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
        try {
            final Economy economy = getVaultEconomy();
            if (economy == null) {
                if (tradeWithMoney) {
                    getLogger().log(Level.WARNING, "Cannot find any economy plugin!");
                }
                economyName = "None";
            } else {
                if (tradeWithMoney) {
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
                economyName = economy.getName();
            }
            hasVault = true;
        } catch (NoClassDefFoundError e) {
            getLogger().log(Level.WARNING, "Cannot find Vault!");
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
            FileUtils.copy(getResource("Messages_es.properties"), new File(languageFolder, "Messages_es.properties"));
            FileUtils.copy(getResource("Messages_fr.properties"), new File(languageFolder, "Messages_fr.properties"));
            FileUtils.copy(getResource("Messages_hu.properties"), new File(languageFolder, "Messages_hu.properties"));
            FileUtils.copy(getResource("Messages_it.properties"), new File(languageFolder, "Messages_it.properties"));
            FileUtils.copy(getResource("Messages_pl.properties"), new File(languageFolder, "Messages_pl.properties"));
            FileUtils.copy(getResource("Messages_ru.properties"), new File(languageFolder, "Messages_ru.properties"));
            FileUtils.copy(getResource("Messages_zh.properties"), new File(languageFolder, "Messages_zh.properties"));
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
        this.loadConfiguration();
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
     * Increments the successful trades.
     */
    public void incrementSuccessfulTrades() {
        successfulTrades++;
    }

    /**
     * Increments the aborted trades.
     */
    public void incrementAbortedTrades() {
        abortedTrades++;
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
     * Gets the design for the trading inventory.
     *
     * @return The design for the trading inventory.
     */
    public Design getDesign() {
        return design;
    }

    /**
     * Gets the item blacklist.
     *
     * @return The item blacklist.
     */
    public Blacklist getBlacklist() {
        return blacklist;
    }

    /**
     * Gets the inventory util used to create a trading inventory.
     *
     * @return The inventory util used to create a trading inventory.
     */
    public InventoryUtil getInventoryUtil() {
        return inventoryUtil;
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
