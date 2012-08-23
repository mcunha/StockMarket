package com.github.mashlol.Threads;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.github.mashlol.MySQL;
import com.github.mashlol.StockMarket;
import com.github.mashlol.Events.EventInstance;
import com.github.mashlol.Stocks.Stock;
import com.github.mashlol.Stocks.Stocks;

public class StockMarketEventThread extends Thread {

	private boolean loop = true;
	private int loopTimes = 0;
	
	public StockMarketEventThread () throws SQLException{
		super ("StockMarketEventThread");
		
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		try {
			PreparedStatement s = conn.prepareStatement("SELECT looptime FROM looptime");
			ResultSet result = s.executeQuery();
			
			try {
				while (result.next()) {
					loopTimes = result.getInt("looptime");
				}
				s.close();
				result.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} finally {
			if (conn != null){
				conn.close();
			}
		}
	}
	
	public void run() {
		if (StockMarket.randomEventFreq == 0)
			loop = false;
		while (loop) {
			// SLEEP
			try {
				Thread.sleep(60000); // THIS DELAY COULD BE CONFIG'D
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (loop) {
			
				loopTimes++;
	
				// DO SOME EVENT STUFF
				
				if (loopTimes % StockMarket.randomEventFreq == 0) {
					loopTimes = 0;
					Stocks stocks = null;
					try {
						stocks = new Stocks();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if (stocks != null && stocks.numStocks() > 0) {
						Stock stock = stocks.getRandomStock();
						EventInstance ei = new EventInstance();
						try {
							ei.forceRandomEvent(stock);
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	public void finish() throws SQLException {
		loop = false;
		
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		try {
			PreparedStatement s = conn.prepareStatement("UPDATE looptime SET looptime = " + loopTimes);
			s.execute();
			s.close();
		} catch (SQLException e) {
			
		} finally {
			conn.close();
		}
	}
	
	
}
