package com.github.mashlol.Stocks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import com.github.mashlol.MySQL;

public class Stocks {

	private ArrayList<Stock> stock = new ArrayList<Stock>();
	private Random random = new Random();
	
	public Stocks () throws SQLException {

		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT stockID FROM stocks");
			ResultSet result = stmt.executeQuery();
			try {
				while (result.next()) {
					stock.add(new Stock(result.getString("stockID")));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			result.close();
			stmt.close();
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}
	
	public Stock getRandomStock () {
		return stock.get(random.nextInt(stock.size()));
	}
	
	public int numStocks () {
		return stock.size();
	}
	
	public ArrayList<Stock> getStocks(){
		return stock;
	}
}
