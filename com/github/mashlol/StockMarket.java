package com.github.mashlol;
 
import java.util.Vector;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.mashlol.Events.Event;
import com.github.mashlol.Messages.Command;
import com.github.mashlol.Threads.StockMarketDividendThread;
import com.github.mashlol.Threads.StockMarketEventThread;
 
public class StockMarket extends JavaPlugin {
 
	private StockMarketCommandExecutor myExecutor;
	public static Vector<Command> commands = new Vector<Command>();
	
	public static Vector<Event> events = new Vector<Event>();
	
	public static Permission permission = null;
	public static Economy economy = null;
	
	public static String mysqlIP = "localhost";
	public static String mysqlPort = "3306";
	public static String mysqlDB = "sm";
	public static String mysqlUser = "root";
	public static String mysqlPW = "";
	
	public static int dividendFreq = 1440;
	public static int randomEventFreq = 60;
	public static int maxPerPlayer = 250;
	public static int maxPerPlayerPerStock = 50;
	
	public static boolean broadcastEvents = true;
	public static boolean broadcastPayouts = true;
	
	public static boolean payOffline = true;
	
	private Logger log = Logger.getLogger("Minecraft");
	private StockMarketEventThread e;
	private StockMarketDividendThread d;
	public DBContext dbContext = null;
	
	public void onDisable() {
		if (e != null) {
			e.finish();
		}
		if (d != null) {
			d.finish();
		}
		
		dbContext.close();
	}

	public void onEnable() {
		if (setupEconomy()) {
			log.info("[StockMarket] Economy plugin detected and hooked into.");
		} else {
			log.info("[StockMarket] Economy plugin not detected! Disabling StockMarket!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (setupPermissions()) {
			log.info("[StockMarket] Permissions plugin detected and hooked into.");
		} else {
			log.info("[StockMarket] Permissions plugin not detected! Disabling StockMarket!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		myExecutor = new StockMarketCommandExecutor(this);
		getCommand("sm").setExecutor(myExecutor);
		
		
		initCommands();
		
		loadConfiguration();

		dbContext = new DBContext();

		e = new StockMarketEventThread();
		e.start();
		
		d = new StockMarketDividendThread();
		d.start();
	}
	
	public void reload() {
		if (e != null) {
			e.finish();
		}
		if (d != null) {
			d.finish();
		}
		
		dbContext.close();
		
		loadConfiguration();
		
		dbContext = new DBContext();

		e = new StockMarketEventThread();
		e.start();
		
		d = new StockMarketDividendThread();
		d.start();
	}
	
	public void loadConfiguration() {
		FileConfiguration config = getConfig();
		
		config.options().copyDefaults(true);
		
		config.addDefault("mysql.ip", mysqlIP);
		config.addDefault("mysql.port", mysqlPort);
		config.addDefault("mysql.database", mysqlDB);
		config.addDefault("mysql.username", mysqlUser);
		config.addDefault("mysql.password", mysqlPW);
		
		config.addDefault("dividend-frequency", dividendFreq);
		config.addDefault("random-event-frequency", randomEventFreq);
		config.addDefault("max-total-stocks-per-player", maxPerPlayer);
		config.addDefault("max-total-stocks-per-player-per-stock", maxPerPlayerPerStock);
		
		config.addDefault("pay-offline-players", payOffline);
		
		config.addDefault("broadcast-events", broadcastEvents);
		config.addDefault("broadcast-payouts", broadcastPayouts);
		
		mysqlIP = config.getString("mysql.ip");
		mysqlPort = config.getString("mysql.port");
		mysqlDB = config.getString("mysql.database");
		mysqlUser = config.getString("mysql.username");
		mysqlPW = config.getString("mysql.password");
		
		dividendFreq = config.getInt("dividend-frequency");
		randomEventFreq = config.getInt("random-event-frequency");
		maxPerPlayer = config.getInt("max-total-stocks-per-player");
		maxPerPlayerPerStock = config.getInt("max-total-stocks-per-player-per-stock");
		
		payOffline = config.getBoolean("pay-offline-players");
		
		broadcastEvents = config.getBoolean("broadcast-events");
		broadcastPayouts = config.getBoolean("broadcast-payouts");

		saveConfig();
		
		// LOAD EVENTS
		events.clear();
		int i = 0;
		while(getConfig().getString("events." + i + ".message") != null) {
			events.add(new Event(getConfig().getString("events." + i + ".message"), getConfig().getBoolean("events." + i + ".up"), getConfig().getInt("events." + i + ".frequency")));
			i++;
		}
	}
	
	private void initCommands() {
		commands.add(new Command("help", "Displays StockMarket help.", "<page>",  "stockMarket.user.help"));
		commands.add(new Command("info", "Displays plugin version & status.", "",  "stockMarket.user.info"));
		commands.add(new Command("list", "Displays a list of stocks you are allowed to buy and their current price.", "",  "stockMarket.user.list"));
		commands.add(new Command("list mine", "Displays a list of stocks that you currently own and their current price.", "",  "stockMarket.user.list"));
		commands.add(new Command("buy", "Buys the stock & amount specified.", "<stockID> <amount>",  "stockMarket.user.buy"));
		commands.add(new Command("sell", "Sells the stock & amount specified.", "<stockID> <amount>",  "stockMarket.user.sell"));
		commands.add(new Command("add", "Adds a new stock to the list of all stocks.", "<stockID> <basePrice> <maxPrice> <minPrice> <volatility> <amount> <dividend> <stockName>",  "stockMarket.admin.add"));
		commands.add(new Command("addshares", "Adds mores shares to an existing stock", "<stockID> <amount> [priceofextrashares]",  "stockMarket.admin.addshares"));
		commands.add(new Command("remove", "Removes an existing stock from the list of all stocks.  Cannot be undone.", "<stockID>",  "stockMarket.admin.remove"));
		commands.add(new Command("set", "Sets all the values of the given stock to the new specified values. Does not affect the current price.", "<stockID> <newBasePrice> <newMaxPrice> <newMinPrice> <newVolatility> <newAmount> <newDividend> <newStockName>",  "stockMarket.admin.set"));
		commands.add(new Command("reload", "Reloads the StockMarket config.", "",  "stockMarket.admin.reload"));
		commands.add(new Command("forcerandom", "Forces a random event to occur on a random stock.", "",  "stockMarket.admin.event"));
		commands.add(new Command("", "Displays more info about stock requested.", "<stockID>",  "stockMarket.user.detail"));
	}
	
	private Boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
	
	private Boolean setupEconomy() {
 		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
        	economy = rsp.getProvider();
        }

        return (economy != null);
    }
}