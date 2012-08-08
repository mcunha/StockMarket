package com.github.mashlol.Stocks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import com.github.mashlol.MySQL;

public class Stock {

	private String name;
	private String stockID;
	private double price;
	private double basePrice;
	private double maxPrice;
	private double minPrice;
	private double volatility;
	private int amount;
	private double dividend;
	
	private boolean exists;
	
	public Stock (String name) throws SQLException {
		this.stockID = name;
		
		exists = getInfo();
	}
	
	private boolean getInfo() throws SQLException {
		
		boolean stock_exists = false;

		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM stocks WHERE stockID LIKE ? ");
		stmt.setString(1, stockID);
		ResultSet result = stmt.executeQuery();
		
		try {
			while (result.next()) {
				// WE FOUND IT, STORE SOME INFO
				name = result.getString("name");
				price = result.getDouble("price");
				basePrice = result.getDouble("basePrice");
				maxPrice = result.getDouble("maxPrice");
				minPrice = result.getDouble("minPrice");
				volatility = result.getDouble("volatility");
				amount = result.getInt("amount");
				dividend = result.getDouble("dividend");
				stock_exists = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		stmt.close();
		result.close();
		conn.close();
		
		return stock_exists;
	}
	
	public boolean add (String name, String stockID, double baseprice, double maxprice, double minprice, double volatility, int amount, double dividend) throws SQLException {
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		try {
			PreparedStatement s = conn.prepareStatement("ALTER TABLE players ADD COLUMN " + stockID + " INT DEFAULT 0");
			s.execute();
			s.close();
		} catch (SQLException e) {
			return false;
		}
		
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO stocks (name, stockID, price, basePrice, maxPrice, minPrice, volatility, amount, dividend) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
		try {
			stmt.setString(1, name);
			stmt.setString(2, stockID);
			stmt.setDouble(3, baseprice);
			stmt.setDouble(4, baseprice);
			stmt.setDouble(5, maxprice);
			stmt.setDouble(6, minprice);
			stmt.setDouble(7, volatility);
			stmt.setInt(8, amount);
			stmt.setDouble(9, dividend);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		stmt.execute();
		stmt.close();
		conn.close();
		
		return true;
	}
	
	public boolean set (String name, String stockID, double baseprice, double maxprice, double minprice, double volatility, int amount, double dividend) throws SQLException {
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		PreparedStatement stmt = conn.prepareStatement("UPDATE stocks SET name = ?, basePrice = ?, maxPrice = ?, minPrice = ?, volatility = ?, amount = ?, dividend = ? WHERE StockID LIKE ?");
		try {
			stmt.setString(1, name);
			stmt.setDouble(2, baseprice);
			stmt.setDouble(3, maxprice);
			stmt.setDouble(4, minprice);
			stmt.setDouble(5, volatility);
			stmt.setInt(6, amount);
			stmt.setDouble(7, dividend);
			stmt.setString(8, stockID);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		stmt.execute();
		stmt.close();
		conn.close();
		
		return true;
	}
	
	public boolean remove () throws SQLException {
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		try {
			PreparedStatement stmt = conn.prepareStatement("DELETE FROM stocks WHERE StockID LIKE ?");
			stmt.execute("ALTER TABLE players DROP COLUMN " + stockID);
			stmt.close();
		} catch (SQLException e1) {
			return false;
		}
		
		PreparedStatement stmt = conn.prepareStatement("DELETE FROM stocks WHERE StockID LIKE ?");
		try {
			stmt.setString(1, stockID);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		stmt.execute();
		stmt.close();
		conn.close();
		
		return true;
	}
	
	public boolean changePrice (double amount) throws SQLException {
		
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		DecimalFormat newFormat = new DecimalFormat("#.##");
		amount =  Double.valueOf(newFormat.format(amount));
		
		double new_total = getPrice()+amount;
		
		PreparedStatement stmt = null;
		if (new_total > getMaxPrice()) {
			stmt = conn.prepareStatement("UPDATE stocks SET price = ? WHERE stockID = ?");
			stmt.setDouble(1, getMaxPrice());
			stmt.setString(2, getID());
		} else if (new_total < getMinPrice()) {
			stmt = conn.prepareStatement("UPDATE stocks SET price = ? WHERE stockID = ?");
			stmt.setDouble(1, getMinPrice());
			stmt.setString(2, getID());
		} else {
			stmt = conn.prepareStatement("UPDATE stocks SET price = price + ? WHERE stockID = ?");
			stmt.setDouble(1, amount);
			stmt.setString(2, getID());
		}
		stmt.execute();
		stmt.close();
		
		// RECORD THE PRICE CHANGE
		stmt = conn.prepareStatement("INSERT INTO stock_history (stockID, price, change_amt, date_created) VALUES (?, ?, ?, ?)");
		java.util.Date date= new java.util.Date();
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date.getTime());
        stmt.setString(1, getID());
		stmt.setDouble(2, new_total);
		stmt.setDouble(3, amount);
		stmt.setString(4, ts);
		stmt.execute();
		stmt.close();
		
		conn.close();
		
		return true;
	}
	
	
	/**
	 * 
	 * @param frequency
	 * @return
	 */
	public double getStockFluctuation( double frequency ){
		double rand = 80 + (int)(Math.random() * ((120 - 80) + 1));
		System.out.print("STOCKS: RANDOM INT: " + rand);
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
		
		System.out.print("STOCKS: fluctuation INT: " + fluc);
		
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
		
		System.out.print("STOCKS: CHANGE: " + d);
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
	
	
	public boolean exists() {
		return this.exists;
	}
	
	public double getMinPrice() {
		return this.minPrice;
	}
	
	public double getMaxPrice() {
		return this.maxPrice;
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
