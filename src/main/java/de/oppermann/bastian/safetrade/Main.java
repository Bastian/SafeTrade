package de.oppermann.bastian.safetrade;

import de.oppermann.bastian.safetrade.commands.TradeCommand;
import de.oppermann.bastian.safetrade.commands.TradeTabCompleter;
import de.oppermann.bastian.safetrade.listener.*;
import de.oppermann.bastian.safetrade.util.*;
import net.milkbowl.vault.economy.Economy;
import org.bstats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
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
     * The design of the trading inventory.
     */
    private Design design;

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

        getCommand("trade").setExecutor(new TradeCommand());
        getCommand("trade").setTabCompleter(new TradeTabCompleter());

        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryCloseListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryDragListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerPickupItemListener(), this);

        // start metrics
        setupCharts(new Metrics(this));

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
     * Setups the custom starts for bStats.
     *
     * @param metrics The metrics class.
     */
    private void setupCharts(Metrics metrics) {
        // language in config
        metrics.addCustomChart(new Metrics.SimplePie("used_language") {
            @Override
            public String getValue() {
                return getConfig().getString("language", "auto");
            }
        });

        metrics.addCustomChart(new Metrics.SimplePie("default_locale") {
            @Override
            public String getValue() {
                return Locale.getDefault().toLanguageTag();
            }
        });

        // encoding in config
        metrics.addCustomChart(new Metrics.SimplePie("encoding") {
            @Override
            public String getValue() {
                return getConfig().getString("encoding", "UTF-8");
            }
        });

        // tradeWithMoney in config
        metrics.addCustomChart(new Metrics.SimplePie("money_enabled") {
            @Override
            public String getValue() {
                return getConfig().getBoolean("tradeWithMoney", true) ? "enabled" : "disabled";
            }
        });

        // noDebts in config
        metrics.addCustomChart(new Metrics.SimplePie("no_debts_enabled") {
            @Override
            public String getValue() {
                return getConfig().getBoolean("noDebts", true) ? "enabled" : "disabled";
            }
        });

        // maxTradingDistance in config
        metrics.addCustomChart(new Metrics.SimplePie("max_trading_distance") {
            @Override
            public String getValue() {
                return String.valueOf(getConfig().getInt("maxTradingDistance", 15));
            }
        });

        // tradeThroughWorlds in config
        metrics.addCustomChart(new Metrics.SimplePie("trade_through_worlds_enabled") {
            @Override
            public String getValue() {
                return getConfig().getBoolean("tradeThroughWorlds", false) ? "enabled" : "disabled";
            }
        });

        // fastTrade in config
        metrics.addCustomChart(new Metrics.SimplePie("fast_trade_enabled") {
            @Override
            public String getValue() {
                return getConfig().getBoolean("fastTrade", false) ? "enabled" : "disabled";
            }
        });

        // successful trades
        metrics.addCustomChart(new Metrics.SingleLineChart("successful_trades") {
            @Override
            public int getValue() {
                return successfulTrades;
            }
        });

        // aborted trades
        metrics.addCustomChart(new Metrics.SingleLineChart("aborted_trades") {
            @Override
            public int getValue() {
                return abortedTrades;
            }
        });

        // A map which shows in which countries is traded most
        metrics.addCustomChart(new Metrics.AdvancedMapChart("most_active_trading_regions") {
            @Override
            public HashMap<Metrics.Country, Integer> getValues(HashMap<Metrics.Country, Integer> valueMap) {
                valueMap.put(Metrics.Country.AUTO_DETECT, abortedTrades + successfulTrades);
                return valueMap;
            }
        });

        // Is Vault used?
        metrics.addCustomChart(new Metrics.SimplePie("vault_used") {
            @Override
            public String getValue() {
                return hasVault ? "Used" : "Not used";
            }
        });

        // The used economy plugin
        metrics.addCustomChart(new Metrics.SimplePie("economy_plugin") {
            @Override
            public String getValue() {
                return economyName;
            }
        });

        // Download source
        metrics.addCustomChart(new Metrics.SimplePie("download_source") {
            @Override
            public String getValue() {
                // I will change this when before compiling
                return "spigotmc.org";
            }
        });
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
            FileUtils.copy(getResource("Messages_it.properties"), new File(languageFolder, "Messages_it.properties"));
            FileUtils.copy(getResource("Messages_pl.properties"), new File(languageFolder, "Messages_pl.properties"));
            FileUtils.copy(getResource("Messages_ru.properties"), new File(languageFolder, "Messages_ru.properties"));
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
