package com.github.mashlol;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.mashlol.Events.EventInstance;
import com.github.mashlol.Messages.Message;
import com.github.mashlol.Stocks.PlayerStocks;
import com.github.mashlol.Stocks.Stock;
import com.github.mashlol.Stocks.Stocks;

public class StockMarketCommandExecutor implements CommandExecutor {

	private StockMarket plugin;
	 
	/**
	 * 
	 * @param plugin
	 */
	public StockMarketCommandExecutor(StockMarket plugin) {
		this.plugin = plugin;
	}
 
	
	/**
	 * 
	 */
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		
		Message m = new Message(player);
		
		if (command.getName().equalsIgnoreCase("sm")){
			
			/**
			 * /sm help
			 * List plugin command help
			 */
			if (args.length >= 1 && args[0].equalsIgnoreCase("help") && (player == null || StockMarket.permission.has(player, "stockmarket.user.help"))) {
				int page = 1;
				if (args.length > 1) {
					try {
						page = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						m.errorMessage("Invalid Syntax. /sm help for help.");
						return true;
					}
				}
				m.displayHelp(page);
				
			/**
			 * /sm list mine
			 * List all the stocks the player currently has.
			 */
			} else if (args.length >= 2 && args[0].equalsIgnoreCase("list") && args[1].equalsIgnoreCase("mine") && player != null && StockMarket.permission.has(player, "stockmarket.user.list")) {
				PlayerStocks ps = null;
				ps = new PlayerStocks(plugin.dbContext, player);
				ps.listMine(plugin.dbContext);
				
			/**
			 * /sm list history
			 * List stock buy/sell history for the player.
			 */
			} else if (args.length >= 2 && args[0].equalsIgnoreCase("list") && args[1].equalsIgnoreCase("history") && player != null && StockMarket.permission.has(player, "stockmarket.user.list")) {
				PlayerStocks ps = null;
				ps = new PlayerStocks(plugin.dbContext, player);
				ps.listHistory(plugin.dbContext);
				
			/**
			 * /sm list
			 * List all stocks
			 */
			} else if (args.length >= 1 && args[0].equalsIgnoreCase("list") && (player == null || StockMarket.permission.has(player, "stockmarket.user.list"))) {
				// LIST ALL THE STOCKS THIS PLAYER CAN BUY
				PlayerStocks ps = null;
				ps = new PlayerStocks(plugin.dbContext, player);
				ps.listAll(plugin.dbContext);
				
			/**
			 * /sm buy
			 * Buy a stock
			 */
			} else if (args.length >= 2 && args[0].equalsIgnoreCase("buy") && player != null && StockMarket.permission.has(player, "stockmarket.user.buy")) {
				Stock stock = null;
				stock = new Stock(plugin.dbContext, args[1].toUpperCase());
				int amount = 1;
				
				if (args.length == 3) {
					try {
						amount = Integer.parseInt(args[2]);
					} catch (NumberFormatException e) {
						m.errorMessage("Invalid Syntax");
						return true;
					}
				}
				
				if (amount <= 0) {
					m.errorMessage("Invalid amount.");
					return true;
				}
				
				PlayerStocks ps = null;
				ps = new PlayerStocks(plugin.dbContext, player);
				ps.buy(plugin.dbContext, stock, amount);
				
			/**
			 * /sm sell
			 * Sell a stock
			 */
			} else if (args.length >= 2 && args[0].equalsIgnoreCase("sell") && player != null && StockMarket.permission.has(player, "stockmarket.user.sell")) {
				Stock stock = null;
				stock = new Stock(plugin.dbContext, args[1].toUpperCase());
				int amount = 1;
				
				if (args.length == 3) {
					try {
						amount = Integer.parseInt(args[2]);
					} catch (NumberFormatException e) {
						m.errorMessage("Invalid Syntax");
						return true;
					}
				}
				
				if (amount > 0) {
					m.errorMessage("Invalid amount.");
					return true;
				}
				
				PlayerStocks ps = null;
				ps = new PlayerStocks(plugin.dbContext, player);
				ps.sell(plugin.dbContext, stock, amount);
				
			/**
			 * /sm add
			 * Add a stock
			 */
			} else if (args.length >= 9 && args[0].equalsIgnoreCase("add") && (player == null || StockMarket.permission.has(player, "stockmarket.admin.add"))) {
				final String stockID = args[1];
				final double baseprice;
				final double minprice;
				final double maxprice;
				final double volatility;
				final int amount;
				final double dividend;
				try {
					baseprice = Double.parseDouble(args[2]);
					maxprice = Double.parseDouble(args[3]);
					minprice = Double.parseDouble(args[4]);
					volatility = Double.parseDouble(args[5]);
					amount = Integer.parseInt(args[6]);
					dividend = Double.parseDouble(args[7]);
				} catch (NumberFormatException e) {
					m.errorMessage("Invalid syntax.");
					return true;
				}
				
				if (amount < -1) {
					m.errorMessage("Invalid amount.");
					return true;
				}
				
				String name = args[8];
				for (int i=9; i<args.length; i++) {
					name += " ";
					name += args[i];
				}
				
				Stock stock = null;
				stock = new Stock(plugin.dbContext, stockID);
				
				if (stock.exists()) {
					m.errorMessage("A stock with that ID already exists!");
					return true;
				}
				
				if (stock.add(plugin.dbContext, name, stockID, baseprice, maxprice, minprice, volatility, amount, dividend))
					m.successMessage("Successfully created new stock.");
				else
					m.errorMessage("Failed to create new stock.");
				
			/**
			 * /sm remove
			 * Delete a stock
			 */
			} else if (args.length == 2 && args[0].equalsIgnoreCase("remove") && (player == null || StockMarket.permission.has(player, "stockmarket.admin.remove"))) {
				String stockID = args[1];
				
				Stock stock = null;
				stock = new Stock(plugin.dbContext, stockID);
				
				if (!stock.exists()) {
					m.errorMessage("That stock does not exist.");
					return true;
				}

				stock.remove(plugin.dbContext);
				m.successMessage("Successfully removed that stock.");
				
			} else if (args.length >= 8 && args[0].equalsIgnoreCase("set") && (player == null || StockMarket.permission.has(player, "stockmarket.admin.set"))){
				final String stockID = args[1];
				final double baseprice;
				final double minprice;
				final double maxprice;
				final double volatility;
				final int amount;
				final double dividend;
				try {
					baseprice = Double.parseDouble(args[2]);
					maxprice = Double.parseDouble(args[3]);
					minprice = Double.parseDouble(args[4]);
					volatility = Double.parseDouble(args[5]);
					amount = Integer.parseInt(args[6]);
					dividend = Double.parseDouble(args[7]);
				} catch (NumberFormatException e) {
					m.errorMessage("Invalid syntax.");
					return true;
				}
				
				if (amount < -1) {
					m.errorMessage("Invalid amount.");
					return true;
				}
				
				String name = args[8];
				for (int i=9; i<args.length; i++) {
					name += " ";
					name += args[i];
				}
				
				Stock stock = null;
				stock = new Stock(plugin.dbContext, stockID);
						
				if (!stock.exists()) {
					m.errorMessage("Failed to adjust stock.  Make sure the ID was valid.");
					return true;
				}

				if (stock.set(plugin.dbContext, name, stockID, baseprice, maxprice, minprice, volatility, amount, dividend))
					m.successMessage("Successfully adjusted stock.");
				else
					m.errorMessage("Failed to adjust stock.  Make sure the ID was valid.");
				
			/**
			 * /sm addshares
			 * Add shares and dilute existing stock
			 */
			} else if (args.length >= 8 && args[0].equalsIgnoreCase("addshares") && (player == null || StockMarket.permission.has(player, "stockmarket.admin.addshares"))){
				String stockID = args[1];
				int extraAmount;
				double extraSharePps = 0.0;

				try {
					extraAmount = Integer.parseInt(args[2]);
				} catch (NumberFormatException e) {
					m.errorMessage("Invalid syntax for the extra amount of shares.");
					return true;
				}

				if (args.length > 2) {
					try {
						extraSharePps = Double.parseDouble(args[3]);
					} catch (NumberFormatException e) {
						m.errorMessage("Invalid syntax for the price of the extra shares.");
						return true;
					}
				}

				if (extraAmount < 1) {
					m.errorMessage("Invalid amount of extra shares.");
					return true;
				}

				if (extraSharePps < 0.0) {
					m.errorMessage("Invalid price for the extra shares.");
					return true;
				}

				Stock stock = null;
				stock = new Stock(plugin.dbContext, stockID);
				
				if (!stock.exists()) {
					m.errorMessage("Failed to adjust stock.  Make sure the ID was valid.");
					return true;
				}
						
				if (stock.dilute(plugin.dbContext, extraAmount, extraSharePps))
					m.successMessage("Successfully added shares to stock.");
				else
					m.errorMessage("Error while diluting stock.");
			} else if (args.length == 1 && args[0].equalsIgnoreCase("reload") && (player == null || StockMarket.permission.has(player, "stockmarket.admin.reload"))) { 
				plugin.reloadConfig();
				plugin.reload();
				m.successMessage("Successfully reloaded StockMarket.");
			}  else if (args.length == 1 && args[0].equalsIgnoreCase("forcerandom") && (player == null || StockMarket.permission.has(player, "stockmarket.admin.event"))) {
				Stocks s = null;
				s = new Stocks(plugin.dbContext);
				if (s.numStocks() > 0) {
					EventInstance ei = new EventInstance();
					ei.forceRandomEvent(plugin.dbContext, s.getRandomStock());
				}
			} else if (args.length == 1 && (player == null || StockMarket.permission.has(player, "stockmarket.user.detail"))) {
				// CHECK IF THIS IS A STOCK NAME
				String stockID = args[0];
				
				Stock stock = null;
				stock = new Stock(plugin.dbContext, stockID);
				
				if (!stock.exists()) {
					m.unknownCommand();
					return true;
				}
				
				m.successMessage(stock.toString());
				m.regularMessage("Current Price: " + stock.getPrice());
				
				// BASE SHOULD ONLY DISPLAY FOR A SPECIAL PERMISSION NODE
				if (player == null || StockMarket.permission.has(player, "stockmarket.admin.baseprice"))
					m.regularMessage("Base Price: " + stock.getBasePrice());
				m.regularMessage("Dividend: " + stock.getDividend() + "% per stock.");
				
				if (stock.getAmount() != -1) 
					m.regularMessage("Current Amount: " + stock.getAmount());
				else
					m.regularMessage("Current Amount: Infinite");
				
			} else if (args.length > 0){
				// UNKNOWN COMMAND
				m.unknownCommand();
			} else {
				m.displayInfo();
			}
		}
		
		return true;
	}
}