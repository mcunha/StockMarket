package com.github.mashlol.Stocks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.github.mashlol.MySQL;
import com.github.mashlol.RandomUtils;

public class Stocks {

	private ArrayList<Stock> stock = new ArrayList<Stock>();
	
	public Stocks () throws SQLException {

		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		
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
		conn.close();
	}
	
	public Stock getRandomStock () {
		return stock.get(RandomUtils.getRandomNumber(stock.size()));
	}
	
	public int numStocks () {
		return stock.size();
	}
	
	public ArrayList<Stock> getStocks(){
		return stock;
	}
}
