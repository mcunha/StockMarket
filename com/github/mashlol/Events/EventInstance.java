package com.github.mashlol.Events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.github.mashlol.StockMarket;
import com.github.mashlol.Stocks.Stock;
import com.github.mashlol.Stocks.Stocks;

public class EventInstance {
	

	/*
	 * Instance RNG once per runtime, anymore is overkill
	 */
	static Random randomGenerator = new Random();

	/**
	 * Gets a random number
	 * @return
	 */
	public static int getRandomNumber( Integer max ){
		return randomGenerator.nextInt(max);
	}
	
	
	/**
	 * 
	 * @param data
	 * @param weight
	 * @return
	 */
	public static int getWeightedRandomNumber( int[] data, int[] weight ){
		int totalWeight = sum(weight);
		int n=randomGenerator.nextInt(totalWeight);
		int runningTotal=0;
		for (int i=0;i<weight.length;i++){
		  runningTotal+=weight[i];
		  if (n<runningTotal) return data[i];
		}
		return -1;
	}
	static int sum(int[] a){
		int s=0;
		for (int i=0;i<a.length;i++) s+=a[i];
		return s;
	}

	
	/**
	 * 
	 * @param items
	 * @return
	 */
	public static Event chooseOnWeight(List<Event> items) {
		
		// Set total weight for items
        double completeWeight = 0.0d;
        for (Event item : items){
            completeWeight += item.getFrequency();
        }

        // Choose a random item
        double rand = randomGenerator.nextDouble();
        double r = rand * completeWeight;

        double countWeight = 0.0;
        for (Event item : items) {
            countWeight += item.getFrequency();
            if (countWeight >= r){
                return item;
            }
        }
        return null;
    }

	
	/**
	 * 
	 * @param s
	 * @return
	 * @throws SQLException 
	 */
	public boolean forceRandomEvent(Stock s) throws SQLException {
		
		int rnum = getRandomNumber(2000);
		if(rnum == 3){
			
			// Stock market crash
			Stocks st = new Stocks();
			ArrayList<Stock> stocks = st.getStocks();

			Iterator<Stock> itr = stocks.iterator();
			while(itr.hasNext()){
				Stock s_temp = itr.next();
				s_temp.changePrice( s_temp.getMarketCrashPriceChange() );
			}
			broadcastMessage( "STOCK MARKET CRASH! All stocks lose 75% of value... market in shambles..." );
			
		}
		else if(rnum == 22){
			
			// Stock market bubble
			Stocks st = new Stocks();
			ArrayList<Stock> stocks = st.getStocks();

			Iterator<Stock> itr = stocks.iterator();
			while(itr.hasNext()){
				Stock s_temp = itr.next();
				s_temp.changePrice( s_temp.getMarketBubblePriceChange() );
			}
			broadcastMessage( "STOCKS SURGE WITH TECH BUBBLE! All stocks increase by 25% of value..." );
			
		} else {
		
			Event e = chooseOnWeight( StockMarket.events );
			
			double price_change = s.updatePrice(e.getUp(), e.getFrequency());
			
			String chg_msg = "";
			if(price_change > 0){
				chg_msg = ChatColor.GREEN + "+" + round(price_change);
			} else {
				chg_msg = ChatColor.RED + "" + round(price_change);
			}
			
			broadcastMessage( e.getMessage().replaceAll("%s", s.getID()) + " CHG: " + chg_msg );
			s.changePrice(price_change);
			
		}
		return true;
	}
	
	
	/**
     * 
     * @param val
     * @return
     */
	public float round( Double val ){
    	return (float) (Math.round( val *100.0) / 100.0);
    }
	
	
	/**
	 * 
	 * @param message
	 */
	private void broadcastMessage (String message) {
		if (StockMarket.broadcastEvents)
			Bukkit.getServer().broadcastMessage(ChatColor.WHITE + "[" + ChatColor.GOLD + "Stocks" + ChatColor.WHITE + "] " + ChatColor.DARK_GREEN + message);
	}
}
