package com.github.mashlol.Stocks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.github.mashlol.MySQL;
import com.github.mashlol.StockMarket;
import com.github.mashlol.Messages.Message;

public class PlayerStocks {

	private Player player;
	private HashMap<String, PlayerStock> stock = new HashMap<String, PlayerStock>();
	private boolean exists;
	private String playerName;
	
	public PlayerStocks (Player player) {
		this.player = player;
		if (player != null)
			this.playerName = player.getName();
		else
			this.playerName = "";
		
		exists = getPlayerInfo();
	}
	
	public PlayerStocks (String playerName) {
		this.player = null;
		this.playerName = playerName;
		
		exists = getPlayerInfo();
	}
	
	private boolean getPlayerInfo() {
		// FIND THIS PLAYER IN THE DB, FILL IN HIS INFO
		MySQL mysql = new MySQL();
		
		// NOW LETS FIND EM
		PreparedStatement stmt = mysql.prepareStatement("SELECT * FROM players WHERE name LIKE ? ");
		try {
			stmt.setString(1, playerName);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ResultSet result = mysql.query(stmt);
		
		
		
		try {
			while (result.next()) {
				// WE FOUND IT, STORE SOME INFO
				stmt = mysql.prepareStatement("SELECT stockID FROM stocks");
				ResultSet result2 = mysql.query(stmt);
				while (result2.next()) {
					PlayerStock newS = new PlayerStock();
					
					newS.stock = new Stock(result2.getString("stockID"));
					newS.amount = result.getInt(newS.stock.toID());
					
					this.stock.put(newS.stock.getID().toUpperCase(), newS);
				}
				
				mysql.close();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// WE DIDNT FIND IT, LETS CREATE IT
		stmt = mysql.prepareStatement("INSERT INTO players (name) Values(?)");
		try {
			stmt.setString(1, playerName);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		mysql.execute(stmt);
		
		
		mysql.close();
		return false;
	}
	
	public boolean exists() {
		return this.exists;
	}
	
	public boolean sell (Stock stock, int amount) {
		Message m = new Message(player);
		
		if (stock.exists()) {
				// CHECK THE PLAYER HAS ENOUGH TO SELL
				if (this.stock.get(stock.getID()).amount - amount < 0) {
					m.errorMessage("Failed to sell!  Check that you have that many!");
					return false;
				}
				
				// OKAY THEY DO, LETS SELL EM
				this.stock.get(stock.getID()).amount -= amount;
				
				// OK NOW LETS UPDATE THE DATABASE
				MySQL mysql = new MySQL();
				PreparedStatement stmt = mysql.prepareStatement("UPDATE players SET " + stock.getID() + " = ? WHERE name LIKE ?");
				try {
					stmt.setInt(1, this.stock.get(stock.getID()).amount);
					stmt.setString(2, player.getName());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				mysql.execute(stmt);
				
				// DETERMINE WHICH STOCKS ARE BEING SOLD, UPDATE THE AMOUNT_SOLD
				
				int amount_selling = amount;

				stmt = mysql.prepareStatement("SELECT *, amount - amount_sold as diff FROM player_stock_transactions WHERE player = ? AND amount_sold < amount AND trxn_type = 'Buy' ORDER BY id");
				try {
					stmt.setString(1, player.getName());
					ResultSet result = mysql.query(stmt);
					while (result.next()) {
						
						int sold_this_round = 0;
						
						if(amount_selling > 0){
						
							// if we're selling less than the stock amount for the current buy trxn
							if(amount_selling <= result.getInt("diff")){
								sold_this_round = amount_selling;
							} else {
								// otherwise, we're selling more but can't exceed the diff at this point
								sold_this_round = result.getInt("diff");
							}
							
//							System.out.println("[STOCK DEBUG] Selling total " + amount_selling + " - ID: " + result.getInt("id") + " Diff " + result.getInt("diff") + " sold this round: " + sold_this_round);
							
							// reduce the total amount selling by what was sold this round
							amount_selling -= sold_this_round;
							
							// set amount_sold for these purchases
							stmt = mysql.prepareStatement("UPDATE player_stock_transactions SET amount_sold = amount_sold + ? WHERE id = ?");
							try {
								stmt.setInt(1, sold_this_round);
								stmt.setInt(2, result.getInt("id"));
								mysql.execute(stmt);
							} catch (SQLException e) {
								e.printStackTrace();
								return false;
							}
							
							// The difference per stock
							double buy_sell_diff = stock.getPrice() - result.getDouble("price");
							
							// STORE THE SELL PRICE TRANSACTION
							stmt = mysql.prepareStatement("" +
									"INSERT INTO player_stock_transactions (player, stockID, trxn_type, price, amount, amount_sold, unit_difference, total_difference)" +
									"VALUES (?, ?, 'Sell', ?, ?, 0, ?, ?)");
							try {
								stmt.setString(1, player.getName());
								stmt.setString(2, stock.getID());
								stmt.setDouble(3, stock.getPrice());
								stmt.setInt(4, amount);
								stmt.setDouble(5, buy_sell_diff);
								stmt.setDouble(6, (buy_sell_diff * sold_this_round) );
							} catch (SQLException e) {
								e.printStackTrace();
								return false;
							}
							
							mysql.execute(stmt);
							
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				
				
				// UPDATE AMOUNT IF NOT INFINITE
				if (stock.getAmount() != -1) {
					stmt = mysql.prepareStatement("UPDATE stocks SET amount = amount + ? WHERE StockID LIKE ?");
					try {
						stmt.setInt(1, amount);
						stmt.setString(2, stock.getID());
					} catch (SQLException e) {
						e.printStackTrace();
						return false;
					}
					
					mysql.execute(stmt);
				}
				
				
				mysql.close();
				
				StockMarket.economy.depositPlayer(player.getName(), amount * stock.getPrice());
				m.successMessage("Successfully sold " + amount + " " + stock + " stocks for " + stock.getPrice() + " " + StockMarket.economy.currencyNamePlural() + " each.");
				return true;
		} else {
			m.errorMessage("Invalid stock ID");
			return false;
		}
	}
	
	public boolean buy (Stock stock, int amount) {
		Message m = new Message(player);
		
		if (stock.exists()) {
			if ((stock.getAmount() >= amount || stock.getAmount() == -1)) {
				
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
				MySQL mysql = new MySQL();
				PreparedStatement stmt = mysql.prepareStatement("UPDATE players SET " + stock.getID() + " = ? WHERE name LIKE ?");
				try {
					stmt.setInt(1, this.stock.get(stock.getID()).amount);
					stmt.setString(2, player.getName());
				} catch (SQLException e) {
					e.printStackTrace();
					return false;
				}
				
				mysql.execute(stmt);
				
				// STORE THE BUY PRICE TRANSACTION
				stmt = mysql.prepareStatement("INSERT INTO player_stock_transactions (player, stockID, trxn_type, price, amount, amount_sold) VALUES (?, ?, 'Buy', ?, ?, 0)");
				try {
					stmt.setString(1, player.getName());
					stmt.setString(2, stock.getID());
					stmt.setDouble(3, stock.getPrice());
					stmt.setInt(4, amount);
				} catch (SQLException e) {
					e.printStackTrace();
					return false;
				}
				
				mysql.execute(stmt);
				
				// UPDATE AMOUNT IF NOT INFINITE
				if (stock.getAmount() != -1) {
					stmt = mysql.prepareStatement("UPDATE stocks SET amount = amount - ? WHERE StockID LIKE ?");
					try {
						stmt.setInt(1, amount);
						stmt.setString(2, stock.getID());
					} catch (SQLException e) {
						e.printStackTrace();
						return false;
					}
					
					mysql.execute(stmt);
				}
				
				mysql.close();
				
				StockMarket.economy.depositPlayer(player.getName(), -1 * amount * stock.getPrice());
				m.successMessage("Successfully purchased " + amount + " " + stock + " stocks for " + stock.getPrice() + " " + StockMarket.economy.currencyNamePlural() + " each.");
				return true;
			} else {
				m.errorMessage("There is not enough of that stock left to buy that many!");
				return false;
			}
		} else {
			m.errorMessage("Invalid stock ID");
			return false;
		}
	}
	
	public void listAll () {
		Message m = new Message(player);
		DecimalFormat newFormat = new DecimalFormat("#.##");
		
		m.successMessage("List of stocks:");
		for (PlayerStock ps : stock.values())
			m.regularMessage(ps.stock.getID() + " - $" + ChatColor.AQUA + newFormat.format(ps.stock.getPrice()) + ChatColor.WHITE + " - Qty: " + ps.stock.getAmount() + ChatColor.WHITE + " - " + ps.stock.getName());
	}
	
	public void listMine () throws SQLException {
		Message m = new Message(player);
		DecimalFormat newFormat = new DecimalFormat("#.##");
		
		if (!hasStocks()) {
			m.errorMessage("You don't own any stocks. /sm help for help.");
			return;
		}
		
		m.successMessage("List of your stocks (estimated returns):");
		for (PlayerStock ps : stock.values())
			if (ps.amount > 0){
				
				DecimalFormat diffFormat = new DecimalFormat("#.####");
				double purch_price = ps.getPurchasePrice(player);
				
				double stock_diff = (ps.stock.getPrice() - purch_price);
				String diff = diffFormat.format( stock_diff );
				double returns_price = stock_diff * ps.amount;
				String returns = diffFormat.format(returns_price);
				
				ChatColor stock_diff_color = ChatColor.RED;
				if(stock_diff > 0){
					stock_diff_color = ChatColor.GREEN;
				}
				
				ChatColor returns_color = ChatColor.RED;
				if(returns_price > 0){
					returns_color = ChatColor.GREEN;
				}
				
				String chat_msg = ps.stock.getID();
				chat_msg += " $" + ChatColor.AQUA + newFormat.format(ps.stock.getPrice()) + ChatColor.WHITE;
				chat_msg += ". Paid $" + ChatColor.GRAY + purch_price + ChatColor.WHITE + " for " + ps.amount;
				chat_msg += " [" + stock_diff_color + diff + ChatColor.WHITE + "]";
				chat_msg += " Rtrn: $" + returns_color + returns + ChatColor.WHITE;
				
				m.regularMessage( chat_msg );
			}
	}
	
	
	public void listHistory () throws SQLException {
		Message m = new Message(player);
		DecimalFormat newFormat = new DecimalFormat("#.##");
		
		m.successMessage("Stock transaction history:");
		
		// WE FOUND IT, STORE SOME INFO
		MySQL mysql = new MySQL();
		
		PreparedStatement stmt = mysql.prepareStatement("SELECT * FROM player_stock_transactions WHERE player = ? ORDER BY id");
		try {
			stmt.setString(1, player.getName());
			ResultSet result = mysql.query(stmt);
			while (result.next()) {
				m.regularMessage( "("+result.getString("trxn_type")+") " + result.getInt("amount") + " " + result.getString("stockID") + " at " + newFormat.format(result.getDouble("price")) );
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		mysql.close();

	}
	
	public boolean payoutDividends () {
		for (PlayerStock ps : stock.values()) 
				StockMarket.economy.depositPlayer(playerName, ps.amount * ps.stock.getDividend() * .01 * ps.stock.getPrice());
		
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
		return stock.get(s.getID()).amount;
	}
	
	public boolean hasStocks () {
		for (PlayerStock ps : stock.values())
			if (ps.amount > 0)
				return true;
		
		return false;
	}
	
}
