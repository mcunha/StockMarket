package com.github.mashlol.Events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;

import com.github.mashlol.MoneyUtils;
import com.github.mashlol.RandomUtils;
import com.github.mashlol.StockMarket;
import com.github.mashlol.Stocks.Stock;
import com.github.mashlol.Stocks.Stocks;

public class EventInstance {
	/**
	 * Randomly pick an event based on its frequency
	 * @param items list of events to choose from
	 * @return selected event from items
	 */
	public static Event chooseOnWeight(List<Event> items) {
		
		// Set total weight for items
        int completeWeight = 0;
        for (Event item : items){
            completeWeight += item.getFrequency();
        }

        // Choose a random number
        int r = RandomUtils.getRandomNumber(completeWeight);

        // Find the event that corresponds to our number
        int countWeight = 0;
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
		
		int rnum = RandomUtils.getRandomNumber(2000);
		
		if (rnum == 3) {
			
			// Stock market crash
			Stocks st = new Stocks();
			ArrayList<Stock> stocks = st.getStocks();

			Iterator<Stock> itr = stocks.iterator();
			while(itr.hasNext()){
				Stock s_temp = itr.next();
				s_temp.changePrice( s_temp.getMarketCrashPriceChange() );
			}
			StockMarket.broadcastEventMessage("STOCK MARKET CRASH! All stocks lose 75% of value... market in shambles...");
			
		} else if (rnum == 22) {
			
			// Stock market bubble
			Stocks st = new Stocks();
			ArrayList<Stock> stocks = st.getStocks();

			Iterator<Stock> itr = stocks.iterator();
			while(itr.hasNext()){
				Stock s_temp = itr.next();
				s_temp.changePrice( s_temp.getMarketBubblePriceChange() );
			}
			StockMarket.broadcastEventMessage("STOCKS SURGE WITH TECH BUBBLE! All stocks increase by 25% of value...");
			
		} else {
		
			Event e = chooseOnWeight( StockMarket.events );
			
			double price_change = s.updatePrice(e.getUp(), e.getFrequency());
			
			String chg_msg = "";
			if(price_change > 0){
				chg_msg = ChatColor.GREEN + "+" + MoneyUtils.format(price_change);
			} else {
				chg_msg = ChatColor.RED + "" + MoneyUtils.format(price_change);
			}
			
			StockMarket.broadcastEventMessage( e.getMessage().replaceAll("%s", s.getID()) + " CHG: " + chg_msg );
			s.changePrice(price_change);
			
		}
		return true;
	}
}