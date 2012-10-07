package com.github.mashlol.Stocks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Player;

import com.github.mashlol.DBContext;

public class PlayerStock {

	public Stock stock;
	public int amount;
	
	public double getPurchasePrice(DBContext ctx, Player player) {
		PreparedStatement stmt = null;
		ResultSet result = null;
		try {
			stmt = ctx.PrepareStatementRead("SELECT price FROM player_stock_transactions WHERE stockID = ? AND player = ? AND trxn_type = 'Buy' ORDER BY id DESC LIMIT 1");
			stmt.setString(1, this.stock.getID());
			stmt.setString(2, player.getName());
			result = ctx.executeQuery(stmt);
			if (result != null) {
				while (result.first()) {
					return result.getDouble("price");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			ctx.close(result);
			ctx.close(stmt);
		}
		return 0;
	}
	
}
