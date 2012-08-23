package com.github.mashlol.Threads;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.github.mashlol.MySQL;
import com.github.mashlol.StockMarket;
import com.github.mashlol.Stocks.PlayerStocks;

public class StockMarketDividendThread extends Thread {

	private boolean loop = true;
	private int loopTimes = 0;
	
	public StockMarketDividendThread () throws SQLException{
		super ("StockMarketDividendThread");
		
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		PreparedStatement s = conn.prepareStatement("SELECT looptime2 FROM looptime");
		ResultSet result = s.executeQuery();
		
		try {
			while (result.next()) {
				loopTimes = result.getInt("looptime2");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		conn.close();
	}
	
	public void run() {
		if (StockMarket.dividendFreq == 0)
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
				
				if (loopTimes % StockMarket.dividendFreq == 0) {
					broadcastMessage("Paying out all stock dividends");
					
					if (StockMarket.payOffline == true) {
						MySQL mysql = new MySQL();
						Connection conn = mysql.getConn();
						try {
							PreparedStatement s;
							try {
								s = conn.prepareStatement("SELECT name FROM players");
								ResultSet result = s.executeQuery();
								try {
									while (result.next()) {
										String playerName = result.getString("name");
										Player p = Bukkit.getServer().getPlayer(playerName);
										PlayerStocks ps;
										if (p != null)
											 ps = new PlayerStocks(p);
										else
											ps = new PlayerStocks(playerName);
											ps.payoutDividends();
									}
								} catch (SQLException e) {
									
								}
							} catch (SQLException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						} finally {
							if (conn != null) {
								conn.close();
							}
						}
						
						
						
					} else {
						Player[] players = Bukkit.getOnlinePlayers();
		                //loop through all of the online players and give them all a random item and amount of something, The diamond ore breaker will not get a reward.
		                for (Player player : players) {
		                	PlayerStocks ps;
							try {
								ps = new PlayerStocks(Bukkit.getServer().getPlayer(player.getName()));
								ps.payoutDividends();
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
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
			PreparedStatement s = conn.prepareStatement("UPDATE looptime SET looptime2 = " + loopTimes);
			s.execute();
			s.close();
		} catch (SQLException e) {
			
		}
		conn.close();
	}
	
	private void broadcastMessage (String message) {
		if (StockMarket.broadcastPayouts)
			Bukkit.getServer().broadcastMessage(ChatColor.WHITE + "[" + ChatColor.GOLD + "StockMarketPayday" + ChatColor.WHITE + "] " + ChatColor.DARK_GREEN + message);
	}
	
	
}
