package com.github.mashlol.Threads;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.github.mashlol.DBContext;
import com.github.mashlol.StockMarket;
import com.github.mashlol.Events.EventInstance;
import com.github.mashlol.Stocks.Stock;
import com.github.mashlol.Stocks.Stocks;

public class StockMarketEventThread extends Thread {

	private boolean loop = true;
	private int loopTimes = 0;
	private DBContext ctx = null;
	
	public StockMarketEventThread () {
		super ("StockMarketEventThread");
		
		ctx = new DBContext();
		
		ResultSet result = null;
		try {
			result = ctx.executeQueryRead("SELECT looptime FROM looptime");
			while (result.next()) {
				loopTimes = result.getInt("looptime");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			ctx.close(result);
		}
	}
	
	public void run() {
		try {
			if (StockMarket.randomEventFreq == 0)
				loop = false;
			
			while (loop) {
				
				// SLEEP
				Thread.sleep(60000); // THIS DELAY COULD BE CONFIG'D
				
				if (!loop) break;
				loopTimes++;
	
				// DO SOME EVENT STUFF
				
				if (loopTimes % StockMarket.randomEventFreq != 0) continue;
				
				loopTimes = 0;
				Stocks stocks = null;
				stocks = new Stocks(ctx);
				
				if (stocks.numStocks() == 0) continue;
				
				Stock stock = stocks.getRandomStock();
				EventInstance ei = new EventInstance();
				ei.forceRandomEvent(ctx, stock);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public void finish()  {
		loop = false;
		this.interrupt();
		try {
			this.join(5000);
		} catch (InterruptedException e) {
		}
		ctx.executeUpdate("UPDATE looptime SET looptime = " + loopTimes);
		ctx.close();
	}
}