package com.github.mashlol.Events;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.github.mashlol.StockMarket;
import com.github.mashlol.Stocks.Stock;

public class EventInstance {

	private Random random = new Random();
	
	private Event getRandomEvent() {
		int r = random.nextInt(totalPossibilities());
		int i = 0;
		
		Iterator<Event> it = StockMarket.events.iterator();
		while (it.hasNext()) {
			Event e = it.next();
			i += e.getFrequency();
			if (r < i) {
				return e;
			}
		}
		
		return null;
	}
	
	private int totalPossibilities () {
		int i = 0;
		Iterator<Event> it = StockMarket.events.iterator();
		while (it.hasNext()) {
			Event e = it.next();
			i += e.getFrequency();
		}
		
		return i;
	}
	
	public boolean forceRandomEvent(Stock s) {
		Event e = getRandomEvent();
		
		double price_change = s.updatePrice(e.getUp(), e.getFrequency());
		
		String chg_msg = "";
		if(price_change > 0){
			chg_msg = ChatColor.GREEN + "+" + round(price_change);
		} else {
			chg_msg = ChatColor.RED + "" + round(price_change);
		}
		
		broadcastMessage( e.getMessage().replaceAll("%s", s.getID()) + " CHG: " + chg_msg );
		s.changePrice(price_change);
		
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
	
	private void broadcastMessage (String message) {
		if (StockMarket.broadcastEvents)
			Bukkit.getServer().broadcastMessage(ChatColor.WHITE + "[" + ChatColor.GOLD + "Stocks" + ChatColor.WHITE + "] " + ChatColor.DARK_GREEN + message);
	}
	
}
