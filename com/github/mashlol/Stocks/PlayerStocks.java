package com.github.mashlol.Stocks;

import java.sql.Connection;
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
	
	public PlayerStocks (Player player) throws SQLException {
		this.player = player;
		if (player != null)
			this.playerName = player.getName();
		else
			this.playerName = "";
		
		exists = getPlayerInfo();
	}
	
	public PlayerStocks (String playerName) throws SQLException {
		this.player = null;
		this.playerName = playerName;
		
		exists = getPlayerInfo();
	}
	
	private boolean getPlayerInfo() throws SQLException {
		// FIND THIS PLAYER IN THE DB, FILL IN HIS INFO
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		// NOW LETS FIND EM
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE name LIKE ? ");
		try {
			stmt.setString(1, playerName);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ResultSet result = stmt.executeQuery();
		
		try {
			while (result.next()) {
				// WE FOUND IT, STORE SOME INFO
				PreparedStatement stmt_s = conn.prepareStatement("SELECT stockID FROM stocks");
				ResultSet result2 = stmt_s.executeQuery();
				while (result2.next()) {
					PlayerStock newS = new PlayerStock();
					
					newS.stock = new Stock(result2.getString("stockID"));
					newS.amount = result.getInt(newS.stock.toID());
					
					this.stock.put(newS.stock.getID().toUpperCase(), newS);
				}
				
				stmt_s.close();
				result2.close();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		stmt.close();
		
		// WE DIDNT FIND IT, LETS CREATE IT
		stmt = conn.prepareStatement("INSERT INTO players (name) Values(?)");
		try {
			stmt.setString(1, playerName);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		stmt.execute();
		stmt.close();
		
		conn.close();
		return false;
	}
	
	public boolean exists() {
		return this.exists;
	}
	
	public boolean sell (Stock stock, int amount) throws SQLException {
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
				Connection conn = mysql.getConn();
				PreparedStatement stmt = conn.prepareStatement("UPDATE players SET " + stock.getID() + " = ? WHERE name LIKE ?");
				try {
					stmt.setInt(1, this.stock.get(stock.getID()).amount);
					stmt.setString(2, player.getName());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				stmt.execute();
				stmt.close();
				
				// DETERMINE WHICH STOCKS ARE BEING SOLD, UPDATE THE AMOUNT_SOLD
				
				int amount_selling = amount;

				stmt = conn.prepareStatement("SELECT *, amount - amount_sold as diff FROM player_stock_transactions WHERE player = ? AND amount_sold < amount AND trxn_type = 'Buy' AND stockID = ? ORDER BY id");
				try {
					stmt.setString(1, player.getName());
					stmt.setString(2, stock.getID());
					ResultSet result = stmt.executeQuery();
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
							
							System.out.println("[STOCK DEBUG] Selling total " + amount_selling + " - ID: " + result.getInt("id") + " Diff " + result.getInt("diff") + " sold this round: " + sold_this_round);
							
							// reduce the total amount selling by what was sold this round
							amount_selling -= sold_this_round;
							
							// set amount_sold for these purchases
							stmt = conn.prepareStatement("UPDATE player_stock_transactions SET amount_sold = amount_sold + ? WHERE id = ?");
							try {
								stmt.setInt(1, sold_this_round);
								stmt.setInt(2, result.getInt("id"));
								stmt.execute();
							} catch (SQLException e) {
								e.printStackTrace();
								return false;
							}
							
							// The difference per stock
							double buy_sell_diff = stock.getPrice() - result.getDouble("price");
							
							// STORE THE SELL PRICE TRANSACTION
							stmt = conn.prepareStatement("" +
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
							
							stmt.execute();
							stmt.close();
							
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				
				
				// UPDATE AMOUNT IF NOT INFINITE
				if (stock.getAmount() != -1) {
					stmt = conn.prepareStatement("UPDATE stocks SET amount = amount + ? WHERE StockID LIKE ?");
					try {
						stmt.setInt(1, amount);
						stmt.setString(2, stock.getID());
					} catch (SQLException e) {
						e.printStackTrace();
						return false;
					}
					
					stmt.execute();
					stmt.close();
				}
				
				
				conn.close();
				
				StockMarket.economy.depositPlayer(player.getName(), amount * stock.getPrice());
				m.successMessage("Successfully sold " + amount + " " + stock + " stocks for " + stock.getPrice() + " " + StockMarket.economy.currencyNamePlural() + " each.");
				return true;
		} else {
			m.errorMessage("Invalid stock ID");
			return false;
		}
	}
	
	public boolean buy (Stock stock, int amount) throws SQLException {
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
				Connection conn = mysql.getConn();
				PreparedStatement stmt = conn.prepareStatement("UPDATE players SET " + stock.getID() + " = ? WHERE name LIKE ?");
				try {
					stmt.setInt(1, this.stock.get(stock.getID()).amount);
					stmt.setString(2, player.getName());
				} catch (SQLException e) {
					e.printStackTrace();
					return false;
				}
				
				stmt.execute();
				stmt.close();
				
				// STORE THE BUY PRICE TRANSACTION
				stmt = conn.prepareStatement("INSERT INTO player_stock_transactions (player, stockID, trxn_type, price, amount, amount_sold) VALUES (?, ?, 'Buy', ?, ?, 0)");
				try {
					stmt.setString(1, player.getName());
					stmt.setString(2, stock.getID());
					stmt.setDouble(3, stock.getPrice());
					stmt.setInt(4, amount);
				} catch (SQLException e) {
					e.printStackTrace();
					return false;
				}
				
				stmt.execute();
				stmt.close();
				
				// UPDATE AMOUNT IF NOT INFINITE
				if (stock.getAmount() != -1) {
					stmt = conn.prepareStatement("UPDATE stocks SET amount = amount - ? WHERE StockID LIKE ?");
					try {
						stmt.setInt(1, amount);
						stmt.setString(2, stock.getID());
					} catch (SQLException e) {
						e.printStackTrace();
						return false;
					}
					
					stmt.execute();
					stmt.close();
				}
				
				conn.close();
				
				StockMarket.economy.withdrawPlayer(player.getName(), amount * stock.getPrice());
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
			m.errorMessage("You don't own any stocks. Use /sm list, then /sm buy [stock code]");
			return;
		}
		
		m.successMessage("List of your stocks (estimated returns):");
		for (PlayerStock ps : stock.values()){
			if (ps.amount > 0){
				
				String sep = ChatColor.YELLOW + "---" + ChatColor.WHITE;
				String chat_msg = sep+" " + ps.stock.getID() + " Currently $" + ChatColor.AQUA + newFormat.format(ps.stock.getPrice()) + ChatColor.WHITE + " "+sep;
				m.regularMessage( chat_msg );
				
				// query the database for the current stock purchases
				MySQL mysql = new MySQL();
				Connection conn = mysql.getConn();
				PreparedStatement stmt = conn.prepareStatement("SELECT *, amount - amount_sold as remaining FROM player_stock_transactions WHERE player = ? AND stockID = ? AND amount_sold < amount AND trxn_type = 'Buy' ORDER BY id");
				try {
					stmt.setString(1, player.getName());
					stmt.setString(2, ps.stock.getID());
					ResultSet result = stmt.executeQuery();
					
					DecimalFormat diffFormat = new DecimalFormat("#.####");
					double total_returns = 0;
					
					while (result.next()) {
						
						double purch_price = result.getDouble("price");
						int purch_amount = result.getInt("amount");
						int remaining_amount = result.getInt("remaining");
						
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
					
					ChatColor total_returns_color = ChatColor.RED;
					if(total_returns > 0){
						total_returns_color = ChatColor.GREEN;
					}
					
					m.regularMessage( ChatColor.GRAY + " Total " + ps.stock.getID() + " returns: " + total_returns_color + "$" + diffFormat.format(total_returns) );
					
					stmt.close();
					result.close();
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				conn.close();
				
			}
		}
	}
	
	
	public void listHistory () throws SQLException {
		Message m = new Message(player);
		DecimalFormat newFormat = new DecimalFormat("#.##");
		
		m.successMessage("Stock transaction history:");
		
		// WE FOUND IT, STORE SOME INFO
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM player_stock_transactions WHERE player = ? ORDER BY id");
		try {
			stmt.setString(1, player.getName());
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				m.regularMessage( "("+result.getString("trxn_type")+") " + result.getInt("amount") + " " + result.getString("stockID") + " at " + newFormat.format(result.getDouble("price")) );
			}
			result.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		stmt.close();
		conn.close();

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
		PlayerStock ps = stock.get(s.getID());
		return (ps != null ? ps.amount : 0);
	}
	
	public boolean hasStocks () {
		for (PlayerStock ps : stock.values())
			if (ps.amount > 0)
				return true;
		
		return false;
	}
	
}
