package com.github.mashlol.Stocks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import com.github.mashlol.DBContext;
import com.github.mashlol.MySQL;

public class Stock {

	private String name;
	private String stockID;
	private double price;
	private double basePrice;
	private double volatility;
	private int amount;
	private double dividend;
	
	private boolean exists;
	
	public Stock (DBContext ctx, String name) {
		this.stockID = name;
		
		exists = getInfo(ctx);
	}
	
	private boolean getInfo(DBContext ctx) {
		
		boolean stock_exists = false;

		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			stmt = ctx.PrepareStatementRead("SELECT * FROM stocks WHERE stockID LIKE ? ");
			stmt.setString(1, stockID);
			
			result = ctx.executeQuery(stmt);
			if (result != null) {
				while (result.next()) {
					// WE FOUND IT, STORE SOME INFO
					name = result.getString("name");
					price = result.getDouble("price");
					basePrice = result.getDouble("basePrice");
					volatility = result.getDouble("volatility");
					amount = result.getInt("amount");
					dividend = result.getDouble("dividend");
					stock_exists = true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			ctx.close(result);
			ctx.close(stmt);
		}
		
		return stock_exists;
	}
	
	public boolean add (DBContext ctx, String name, String stockID, double baseprice, double maxprice, double minprice, double volatility, int amount, double dividend) {
		PreparedStatement stmt = null;
		try {
			if (ctx.executeUpdate("ALTER TABLE players ADD COLUMN " + stockID + " INT DEFAULT 0") == -1)
				return false;
			
			stmt = ctx.PrepareStatementWrite("INSERT INTO stocks (name, stockID, price, basePrice, maxPrice, minPrice, volatility, amount, dividend) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
			stmt.setString(1, name);
			stmt.setString(2, stockID);
			stmt.setDouble(3, baseprice);
			stmt.setDouble(4, baseprice);
			stmt.setDouble(5, maxprice);
			stmt.setDouble(6, minprice);
			stmt.setDouble(7, volatility);
			stmt.setInt(8, amount);
			stmt.setDouble(9, dividend);
			ctx.execute(stmt);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			ctx.close(stmt);
		}
		
		return true;
	}
	
	public boolean set (DBContext ctx, String name, String stockID, double baseprice, double maxprice, double minprice, double volatility, int amount, double dividend) {
		PreparedStatement stmt = null;
		try {
			stmt = ctx.PrepareStatementWrite("UPDATE stocks SET name = ?, basePrice = ?, maxPrice = ?, minPrice = ?, volatility = ?, amount = ?, dividend = ? WHERE StockID LIKE ?");
			stmt.setString(1, name);
			stmt.setDouble(2, baseprice);
			stmt.setDouble(3, maxprice);
			stmt.setDouble(4, minprice);
			stmt.setDouble(5, volatility);
			stmt.setInt(6, amount);
			stmt.setDouble(7, dividend);
			stmt.setString(8, stockID);

			ctx.execute(stmt);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			ctx.close(stmt);
		}
		
		return true;
	}

	/**
	 * Counts the number of shares currently held by players 
	 * as well as still available for purchase
	 *
	 * @return number of shares issued
	 */
	public int getIssuedShares(DBContext ctx) {
		int playerShares=0;
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			stmt = ctx.PrepareStatementRead("SELECT SUM(amount-amount_sold) as shares_held FROM player_stock_transactionsstocks WHERE StockID LIKE ? AND trxn_type = 'Buy' AND amount_sold < amount");
			stmt.setString(1, stockID);
			
			result = stmt.executeQuery();

			// Should have a single row if there are any shares being held by players
			if (result != null && result.next()) {
				playerShares = result.getInt("shares_held");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			ctx.close(result);
			ctx.close(stmt);
		}

		// Include currently outstanding shares
		return amount + playerShares;
	}

	/**
	 * Dilutes the stock by issuing new shares at a given price. If the price per share for
	 * the extra shares is zero, it works almost like a split, except is doesn't award
	 * new shares to shareholders, thus diluting their position severely.
	 *
	 * @param  extraShares Number of new shares being added.
	 * @param  extraSharePps Price per share of the additional shares.
	 * @return true if successful, false otherwise.
	 */
 	public boolean dilute (DBContext ctx, int extraShares, double extraSharePps) {
 		int originalShares = 0;
		originalShares = this.getIssuedShares(ctx);

 		// Calculate new price as follows:
 		// Overall Stock Valuation = (Original Number of Shares * Current Price) + (Extra Shares * Extra Share Price)
 		// New Share Pool Size = Original Number of Shares + Extra Shares
 		// New share price = Overall Stock Valuation / New Share Pool Size
 		double stockValuation = (originalShares*price)+(extraShares*extraSharePps);
 		double newSharePrice = stockValuation/((double)(originalShares+extraShares));

 		// Update database with new share price, and amount of outstanding shares
 		// NOTE: We don't distribute stock to players
		PreparedStatement stmt = null;
		try {
			stmt = ctx.PrepareStatementWrite("UPDATE stocks SET price = ?, amount = ? WHERE StockID LIKE ?");
			stmt.setDouble(1, newSharePrice);
			stmt.setInt(2, amount+extraShares);
			stmt.setString(3, stockID);
			
			ctx.execute(stmt);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			ctx.close(stmt);
		}
		
		return true;
	}

	
	public boolean remove (DBContext ctx) {
		if (ctx.executeUpdate("ALTER TABLE players DROP COLUMN " + stockID) == -1)
			return false;
		
		PreparedStatement stmt = null;
		
		try {
			stmt = ctx.PrepareStatementWrite("DELETE FROM stocks WHERE StockID LIKE ?");
			stmt.setString(1, stockID);
			ctx.execute(stmt);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			ctx.close(stmt);
		}
		
		return true;
	}
	
	protected static DecimalFormat newFormat = new DecimalFormat("#.##");
	public boolean changePrice (DBContext ctx, double amount) {
		
		amount =  Double.valueOf(newFormat.format(amount));
		double new_total = getPrice()+amount;
		
		PreparedStatement stmt = null;
		try {
			if (new_total <= 1) {
				stmt = ctx.PrepareStatementWrite("UPDATE stocks SET price = 1 WHERE stockID = ?");
				stmt.setString(1, getID());
			} else {
				stmt = ctx.PrepareStatementWrite("UPDATE stocks SET price = price + ? WHERE stockID = ?");
				stmt.setDouble(1, amount);
				stmt.setString(2, getID());
			}
			ctx.execute(stmt);
		
			// RECORD THE PRICE CHANGE
			stmt = ctx.PrepareStatementWrite("INSERT INTO stock_history (stockID, price, change_amt, date_created) VALUES (?, ?, ?, ?)");
			java.util.Date date= new java.util.Date();
	        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date.getTime());
	        stmt.setString(1, getID());
			stmt.setDouble(2, new_total);
			stmt.setDouble(3, amount);
			stmt.setString(4, ts);
			ctx.execute(stmt);
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			ctx.close(stmt);
		}
		
		return true;
	}
	
	
	/**
	 * 
	 * @param frequency
	 * @return
	 */
	public double getStockFluctuation( double frequency ){
		double rand = 80 + (int)(Math.random() * ((120 - 80) + 1));
		return 10*((rand-frequency)/rand)/100;
	}
	
	
	/**
	 * 
	 * @param fluctuation
	 * @return
	 */
	public double getStockFluctuationAmount( double fluctuation ){
		return getPrice() * fluctuation;
	}
	
	
	/**
	 * 
	 * @param up
	 * @param frequency
	 * @return
	 */
	public double updatePrice(boolean up, double frequency) {
		double d = 0;

		double fluc = getStockFluctuation( frequency );
		
		// determine a suitable base price of fluctuation
		double fluc_base = getStockFluctuationAmount( fluc );
		if (up) {
			d = ((double) getVolatility() / 100) * fluc_base;
		} else {
			d = (-1) * ((double) getVolatility() / 100) * fluc_base;
		}
		
		// enforce a minimum price change
		if(d > 0){
			while(d <= 0.20){
				d+=0.10;
			}
		} else {
			while(d >= -0.20){
				d-=0.10;
			}
		}

		return d;
	}
	
	
	/**
	 * 
	 * @param up
	 * @param frequency
	 * @return
	 */
	public double getMarketCrashPriceChange() {
		return (-1) * (getPrice() * 0.75);
	}
	
	
	/**
	 * 
	 * @return
	 */
	public double getMarketBubblePriceChange(){
		return (getPrice() * 0.25);
	}
	
	
	public boolean exists() {
		return this.exists;
	}
	
	public double getBasePrice() {
		return this.basePrice;
	}
	
	public double getPrice() {
		return this.price;
	}
	
	public double getVolatility() {
		return this.volatility;
	}
	
	public String getID() {
		return this.stockID.toUpperCase();
	}
	
	public String getName() {
		return this.name;
	}
	
	public String toID() {
		return this.stockID.toUpperCase();
	}
	
	public String toString() {
		return this.name;
	}
	
	public int getAmount () {
		return this.amount;
	}
	
	public double getDividend () {
		return this.dividend;
	}	
}