package com.github.mashlol.Threads;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.github.mashlol.DBContext;
import com.github.mashlol.StockMarket;
import com.github.mashlol.Stocks.PlayerStocks;

public class StockMarketDividendThread extends Thread {

	private boolean loop = true;
	private int loopTimes = 0;
	private DBContext ctx = null;
	
	public StockMarketDividendThread () {
		super ("StockMarketDividendThread");
		
		ctx = new DBContext();
		ResultSet result = null;
		try {
			result = ctx.executeQueryRead("SELECT looptime2 FROM looptime");
			if (result != null) {
				while (result.next()) {
					loopTimes = result.getInt("looptime2");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				ctx.close(result);
			}
		}
	}
	
	public void run() {
		try {
			if (StockMarket.dividendFreq == 0)
				loop = false;
			
			while (loop) {
				
				// SLEEP
				Thread.sleep(60000); // THIS DELAY COULD BE CONFIG'D
			
				if (!loop) break;
				loopTimes++;
	
				// DO SOME EVENT STUFF
				
				if (loopTimes % StockMarket.dividendFreq != 0) continue;
				
				loopTimes = 0;
				broadcastMessage("Paying out all stock dividends");
				
				// If pay online
				if (StockMarket.payOffline == false) {
					// If not paying offline
					Player[] players = Bukkit.getOnlinePlayers();
	                //loop through all of the online players and give them all a random item and amount of something, The diamond ore breaker will not get a reward.
	                for (Player player : players) {
	                	PlayerStocks ps;
						ps = PlayerStocks.LoadPlayer(ctx, Bukkit.getServer().getPlayer(player.getName()));
						if (ps != null) {
							ps.payoutDividends();
						}
	                }
	                continue;
				}

				// If pay offline
				ResultSet result = null;
				try {
					result = ctx.executeQueryRead("SELECT name FROM players");
					if (result != null) {
						while (result.next()) {
							String playerName = result.getString("name");
							Player p = Bukkit.getServer().getPlayer(playerName);
							PlayerStocks ps;
							if (p != null)
								ps = PlayerStocks.LoadPlayer(ctx, p);
							else
								ps = PlayerStocks.LoadPlayer(ctx, playerName);
							if (ps != null) {
								ps.payoutDividends();
							}
						}
					}
				} catch (SQLException e1) {
					e1.printStackTrace();
				} finally {
					ctx.close(result);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public void finish() {
		loop = false;
		this.interrupt();
		try {
			this.join(5000);
		} catch (InterruptedException e) {
		}
		ctx.executeUpdate("UPDATE looptime SET looptime2 = " + loopTimes);
		ctx.close();
	}
	
	private void broadcastMessage (String message) {
		if (StockMarket.broadcastPayouts)
			Bukkit.getServer().broadcastMessage(ChatColor.WHITE + "[" + ChatColor.GOLD + "StockMarketPayday" + ChatColor.WHITE + "] " + ChatColor.DARK_GREEN + message);
	}
}
