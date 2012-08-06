package com.github.mashlol.Stocks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Player;

import com.github.mashlol.MySQL;

public class PlayerStock {

	public Stock stock;
	public int amount;
	
	
	
	public double getPurchasePrice(Player player) throws SQLException{
		
		
		MySQL mysql = new MySQL();
		Connection conn = mysql.getConn();
		PreparedStatement stmt = conn.prepareStatement("SELECT price FROM player_stock_transactions WHERE stockID = ? AND player = ? AND trxn_type = 'Buy' ORDER BY id DESC LIMIT 1");
		try {
			stmt.setString(1, this.stock.getID());
			stmt.setString(2, player.getName());
			ResultSet result = stmt.executeQuery();
			while (result.first()) {
				return result.getDouble("price");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		stmt.close();
		conn.close();
		return 0;
		
	}
	
}
