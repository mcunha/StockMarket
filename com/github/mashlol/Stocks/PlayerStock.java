package com.github.mashlol.Stocks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Player;

import com.github.mashlol.MySQL;

public class PlayerStock {

	public Stock stock;
	public int amount;
	
	
	
	public double getPurchasePrice(Player player){
		
		
		MySQL mysql = new MySQL();
		
		PreparedStatement stmt = mysql.prepareStatement("SELECT price FROM player_stock_transactions WHERE stockID = ? AND player = ? AND trxn_type = 'Buy' ORDER BY id DESC LIMIT 1");
		try {
			stmt.setString(1, this.stock.getID());
			stmt.setString(2, player.getName());
			ResultSet result = mysql.query(stmt);
			while (result.first()) {
				return result.getDouble("price");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		mysql.close();
		return 0;
		
	}
	
}
