package com.github.mashlol.Stocks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import com.github.mashlol.DBContext;

public class Stocks {

	private ArrayList<Stock> stock = new ArrayList<Stock>();
	private Random random = new Random();
	
	public Stocks (DBContext ctx) {
		ResultSet result=null;
		try {
			result = ctx.executeQueryRead("SELECT stockID FROM stocks");
			if (result != null) {
				while (result.next()) {
					stock.add(new Stock(ctx, result.getString("stockID")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			ctx.close(result);
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
