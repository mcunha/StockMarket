package com.github.mashlol.Stocks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.github.mashlol.DBContext;
import com.github.mashlol.StockMarket;
import com.github.mashlol.Messages.Message;

public class PlayerStocks {

	private Player player;
	private HashMap<String, PlayerStock> stock = new HashMap<String, PlayerStock>();
	private boolean exists;
	private String playerName;
	
	public PlayerStocks (DBContext ctx, Player player) {
		this.player = player;
		if (player != null)
			this.playerName = player.getName();
		else
			this.playerName = "";
		
		exists = getPlayerInfo(ctx);
	}
	
	public PlayerStocks (DBContext ctx, String playerName) {
		this.player = null;
		this.playerName = playerName;
		
		exists = getPlayerInfo(ctx);
	}
	
	private boolean getPlayerInfo(DBContext ctx) {
		// FIND THIS PLAYER IN THE DB, FILL IN HIS INFO
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			// NOW LETS FIND EM
			stmt = ctx.PrepareStatementRead("SELECT * FROM players WHERE name LIKE ? ");
			stmt.setString(1, playerName);
			result = ctx.executeQuery(stmt);
			
			if (result != null) {
				ResultSet result2 = null;
				while (result.next()) {
					// WE FOUND IT, STORE SOME INFO
					try {
						result2 = ctx.executeQueryRead("SELECT stockID FROM stocks");
						if (result2 != null) {
							while (result2.next()) {
								PlayerStock newS = new PlayerStock();
								
								newS.stock = new Stock(ctx, result2.getString("stockID"));
								newS.amount = result.getInt(newS.stock.toID());
								
								this.stock.put(newS.stock.getID().toUpperCase(), newS);
							}
						}
					} finally {
						ctx.close(result2);
					}
					
					return true;
				}
			}
			
			// WE DIDNT FIND IT, LETS CREATE IT
			stmt = ctx.PrepareStatementWrite("INSERT INTO players (name) Values(?)");
			stmt.setString(1, playerName);
			ctx.execute(stmt);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			ctx.close(result);
			ctx.close(stmt);
		}
		return false;
	}
	
	public boolean exists() {
		return this.exists;
	}
	
	protected DecimalFormat moneyFormatter= new DecimalFormat("#.##");
	
	public boolean sell (DBContext ctx, Stock stock, int amount) {
		Message m = new Message(player);
		if (!stock.exists()) {
			m.errorMessage("Invalid stock ID");
			return false;
		}
		
		// CHECK THE PLAYER HAS ENOUGH TO SELL
		if (this.stock.get(stock.getID()).amount - amount < 0) {
			m.errorMessage("Failed to sell!  Check that you have that many!");
			return false;
		}
		
		// OKAY THEY DO, LETS SELL EM
		this.stock.get(stock.getID()).amount -= amount;
		
		// OK NOW LETS UPDATE THE DATABASE
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			stmt = ctx.PrepareStatementWrite("UPDATE players SET " + stock.getID() + " = ? WHERE name LIKE ?");
			stmt.setInt(1, this.stock.get(stock.getID()).amount);
			stmt.setString(2, player.getName());
			ctx.execute(stmt);
		
			// DETERMINE WHICH STOCKS ARE BEING SOLD, UPDATE THE AMOUNT_SOLD
			int amount_selling = amount;
	
			
			// PROCESS ROWS BY MOST PROFITABLE SALES ORDER
			stmt = ctx.PrepareStatementRead("SELECT *, amount - amount_sold as qty_diff, ? - price as price_diff FROM player_stock_transactions WHERE player = ? AND amount_sold < amount AND trxn_type = 'Buy' AND stockID = ? ORDER BY price_diff DESC");
			stmt.setDouble(1, stock.getPrice());
			stmt.setString(2, player.getName());
			stmt.setString(3, stock.getID());
			result = ctx.executeQuery(stmt);
			if (result != null) {
				while (result.next()) {
					int sold_this_round = 0;
					if (amount_selling > 0) {

						// if we're selling less than the stock amount for the current buy trxn
						if (amount_selling <= result.getInt("qty_diff")){
							sold_this_round = amount_selling;
						} else {
							// otherwise, we're selling more but can't exceed the qty_diff at this point
							sold_this_round = result.getInt("qty_diff");
						}
						
						System.out.println("[STOCK DEBUG] Selling total " + amount_selling + " - ID: " + result.getInt("id") + " Price Diff " + moneyFormatter.format(result.getDouble("price_diff")) + " Qty Diff " + result.getInt("qty_diff") + " sold this round: " + sold_this_round);
						
						// reduce the total amount selling by what was sold this round
						amount_selling -= sold_this_round;
						
						// set amount_sold for these purchases
						PreparedStatement stmt2 = null;
						stmt2 = ctx.PrepareStatementWrite("UPDATE player_stock_transactions SET amount_sold = amount_sold + ? WHERE id = ?");
						try {
							stmt2.setInt(1, sold_this_round);
							stmt2.setInt(2, result.getInt("id"));
							if (!ctx.execute(stmt2)) return false;
						
							// The difference per stock
							double buy_sell_diff = stock.getPrice() - result.getDouble("price");
							
							// STORE THE SELL PRICE TRANSACTION
							stmt2 = ctx.PrepareStatementWrite("" +
								"INSERT INTO player_stock_transactions (player, stockID, trxn_type, price, amount, amount_sold, unit_difference, total_difference)" +
								"VALUES (?, ?, 'Sell', ?, ?, 0, ?, ?)");
							stmt2.setString(1, player.getName());
							stmt2.setString(2, stock.getID());
							stmt2.setDouble(3, stock.getPrice());
							stmt2.setInt(4, amount);
							stmt2.setDouble(5, buy_sell_diff);
							stmt2.setDouble(6, (buy_sell_diff * sold_this_round) );
							if (!ctx.execute(stmt2)) return false;
						} finally {
							ctx.close(stmt2);
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			ctx.close(result);
			ctx.close(stmt);
		}
		
		// UPDATE AMOUNT IF NOT INFINITE
		if (stock.getAmount() != -1) {
			try {
				stmt = ctx.PrepareStatementWrite("UPDATE stocks SET amount = amount + ? WHERE StockID LIKE ?");
				stmt.setInt(1, amount);
				stmt.setString(2, stock.getID());
				if (!ctx.execute(stmt)) return false;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			} finally {
				ctx.close(stmt);
			}
		}
		
		StockMarket.economy.depositPlayer(player.getName(), amount * stock.getPrice());
		m.successMessage("Successfully sold " + amount + " " + stock + " stocks for " + stock.getPrice() + " " + StockMarket.economy.currencyNamePlural() + " each.");
		return true;
	}
	
	public boolean buy (DBContext ctx, Stock stock, int amount) {
		Message m = new Message(player);
		
		if (!stock.exists()) {
			m.errorMessage("Invalid stock ID");
			return false;
		}
		
		if ((stock.getAmount() < amount && stock.getAmount() != -1))
		{
			m.errorMessage("There is not enough of that stock left to buy that many!");
			return false;
		}
		
		// CHECK THE PLAYER HAS ENOUGH MONEY TO BUY THIS MANY
		if (StockMarket.economy.getBalance(player.getName()) < amount * stock.getPrice()) {
			m.errorMessage("Not enough money!");
			return false;
		}
		
		// CHECK THE PLAYER ISNT OVER HIS TOTAL LIMIT OF STOCKS
		if (numTotal() + amount > StockMarket.maxPerPlayer) {
			m.errorMessage("Buying that many would put you over the limit for total stocks!");
			return false;
		}
		
		// CHECK THE PLAYER ISNT OVER HIS LIMIT FOR THIS STOCK
		if (numStock(stock) + amount > StockMarket.maxPerPlayerPerStock) {
			m.errorMessage("Buying that many would put you over the limit for that stock!");
			return false;
		}
		
		// OKAY THEY DO, LETS BUY EM
		this.stock.get(stock.getID()).amount += amount;
		
		// OK NOW LETS UPDATE THE DATABASE
		PreparedStatement stmt = null;
		try {
			stmt = ctx.PrepareStatementWrite("UPDATE players SET " + stock.getID() + " = ? WHERE name LIKE ?");
			stmt.setInt(1, this.stock.get(stock.getID()).amount);
			stmt.setString(2, player.getName());
			
			ctx.execute(stmt);
			
			// STORE THE BUY PRICE TRANSACTION
			stmt = ctx.PrepareStatementWrite("INSERT INTO player_stock_transactions (player, stockID, trxn_type, price, amount, amount_sold) VALUES (?, ?, 'Buy', ?, ?, 0)");
			stmt.setString(1, player.getName());
			stmt.setString(2, stock.getID());
			stmt.setDouble(3, stock.getPrice());
			stmt.setInt(4, amount);
			
			ctx.execute(stmt);
			
			// UPDATE AMOUNT IF NOT INFINITE
			if (stock.getAmount() != -1) {
				stmt = ctx.PrepareStatementWrite("UPDATE stocks SET amount = amount - ? WHERE StockID LIKE ?");
				stmt.setInt(1, amount);
				stmt.setString(2, stock.getID());
				
				ctx.execute(stmt);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			ctx.close(stmt);
		}
		
		StockMarket.economy.withdrawPlayer(player.getName(), amount * stock.getPrice());
		m.successMessage("Successfully purchased " + amount + " " + stock + " stocks for " + stock.getPrice() + " " + StockMarket.economy.currencyNamePlural() + " each.");
		return true;
	}
	
	public void listAll () {
		Message m = new Message(player);
		DecimalFormat newFormat = new DecimalFormat("#.##");
		
		m.successMessage("List of stocks:");
		for (PlayerStock ps : stock.values())
			m.regularMessage(ps.stock.getID() + " - $" + ChatColor.AQUA + newFormat.format(ps.stock.getPrice()) + ChatColor.WHITE + " - Qty: " + ps.stock.getAmount() + ChatColor.WHITE + " - " + ps.stock.getName());
	}
	
	protected static DecimalFormat diffFormat = new DecimalFormat("#.####");

	public void listMine (DBContext ctx) {
		Message m = new Message(player);
		DecimalFormat newFormat = new DecimalFormat("#.##");
		
		if (!hasStocks()) {
			m.errorMessage("You don't own any stocks. Use /sm list, then /sm buy [stock code]");
			return;
		}
		
		m.successMessage("List of your stocks (estimated returns):");
		for (PlayerStock ps : stock.values()){
			if (ps.amount <= 0) continue; 
				
			String sep = ChatColor.YELLOW + "---" + ChatColor.WHITE;
			String chat_msg = sep+" " + ps.stock.getID() + " Currently $" + ChatColor.AQUA + newFormat.format(ps.stock.getPrice()) + ChatColor.WHITE + " "+sep;
			m.regularMessage( chat_msg );
			
			PreparedStatement stmt = null;
			ResultSet result = null;
			// query the database for the current stock purchases
			try {
				stmt = ctx.PrepareStatementRead("SELECT stockID, price, SUM(amount) as total_amount, SUM(amount_sold) as total_amount_sold, SUM(amount - amount_sold) as total_amount_remaining FROM player_stock_transactions WHERE player = ? AND stockID = ? AND amount_sold < amount AND trxn_type = 'Buy' GROUP BY stockID, price");
				stmt.setString(1, player.getName());
				stmt.setString(2, ps.stock.getID());
				result = stmt.executeQuery();
				
				double total_returns = 0;
				
				if (result != null) {
					while (result.next()) {
						
						double purch_price = result.getDouble("price");
						int purch_amount = result.getInt("total_amount");
						int remaining_amount = result.getInt("total_amount_remaining");
						
						double stock_diff = (ps.stock.getPrice() - purch_price);
						String diff = diffFormat.format( stock_diff );
						double returns_price = stock_diff * purch_amount;
						total_returns += returns_price;
						String returns = diffFormat.format(returns_price);
						
						ChatColor stock_diff_color = ChatColor.RED;
						if(stock_diff > 0){
							stock_diff_color = ChatColor.GREEN;
						}
	
						ChatColor returns_color = ChatColor.RED;
						if(returns_price > 0){
							returns_color = ChatColor.GREEN;
						}
						
						chat_msg = "Bought " + remaining_amount + " at $" + ChatColor.GRAY + purch_price + ChatColor.WHITE;
						chat_msg += " Chg: " + stock_diff_color + diff + ChatColor.WHITE + "";
						chat_msg += " Rtrn: $" + returns_color + returns + ChatColor.WHITE;
						
						m.regularMessage( chat_msg );
					}
				}
				
				ChatColor total_returns_color = ChatColor.RED;
				if(total_returns > 0){
					total_returns_color = ChatColor.GREEN;
				}
				
				m.regularMessage( ChatColor.GRAY + " Total " + ps.stock.getID() + " returns: " + total_returns_color + "$" + diffFormat.format(total_returns) );
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				ctx.close(result);
				ctx.close(stmt);
			}
		}
	}
	
	
	public void listHistory (DBContext ctx) {
		Message m = new Message(player);
		DecimalFormat newFormat = new DecimalFormat("#.##");
		
		m.successMessage("Stock transaction history:");
		
		// WE FOUND IT, STORE SOME INFO
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			stmt = ctx.PrepareStatementRead("SELECT * FROM player_stock_transactions WHERE player = ? ORDER BY id");
			stmt.setString(1, player.getName());
			result = stmt.executeQuery();
			if (result != null) {
				while (result.next()) {
					m.regularMessage( "("+result.getString("trxn_type")+") " + result.getInt("amount") + " " + result.getString("stockID") + " at " + newFormat.format(result.getDouble("price")) );
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			ctx.close(result);
			ctx.close(stmt);
		}
	}
	
	public boolean payoutDividends () {
		double amount = 0;
		
		// Make one call per player
		for (PlayerStock ps : stock.values()) 
			amount += ps.amount * ps.stock.getDividend() * .01 * ps.stock.getPrice();
		
		// Don't bother if there's no money
		if (amount > 0.0) {
			StockMarket.economy.depositPlayer(playerName, amount);
		}
		
		return true;
	}
	
	private int numTotal () {
		int total = 0;
		for (PlayerStock ps : stock.values()) {
			total += ps.amount;
		}
		return total;
	}

	private int numStock (Stock s) {
		PlayerStock ps = stock.get(s.getID());
		return (ps != null ? ps.amount : 0);
	}
	
	public boolean hasStocks () {
		for (PlayerStock ps : stock.values())
			if (ps.amount > 0)
				return true;
		
		return false;
	}

	public static PlayerStocks LoadPlayer(DBContext ctx, String playerName) {
		return new PlayerStocks(ctx, playerName);
	}

	public static PlayerStocks LoadPlayer(DBContext ctx, Player player) {
		return new PlayerStocks(ctx, player);
	}
}
