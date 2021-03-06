package com.github.mashlol;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {

	private Connection con = null;
	
	
	public Connection getConn(){
		return con;
	}
	
	public MySQL () {
		final String driver = "com.mysql.jdbc.Driver";
		String connection = "jdbc:mysql://" + StockMarket.mysqlIP + ":" + StockMarket.mysqlPort + "/" + StockMarket.mysqlDB;
		final String user = StockMarket.mysqlUser;
		final String password = StockMarket.mysqlPW;
		
		
		try {
			Class.forName(driver);
			con = DriverManager.getConnection(connection, user, password);
			
			
			setUpTables();
			
		} catch (SQLException e) {
			try {
				connection = "jdbc:mysql://" + StockMarket.mysqlIP + ":" + StockMarket.mysqlPort;
				con = DriverManager.getConnection(connection, user, password);
				
//				execute("CREATE DATABASE IF NOT EXISTS " + StockMarket.mysqlDB);
				
				connection = "jdbc:mysql://" + StockMarket.mysqlIP + ":" + StockMarket.mysqlPort + "/" + StockMarket.mysqlDB;
				con = DriverManager.getConnection(connection, user, password);
				
				setUpTables();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void setUpTables() {
//		try {
//			execute("CREATE TABLE IF NOT EXISTS stocks (id int NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name tinytext, stockID tinytext, price decimal(10, 2), basePrice decimal(10, 2), maxPrice decimal(10, 2), minPrice decimal(10, 2), volatility decimal(10, 2), amount int, dividend decimal(10, 2))");
//			execute("CREATE TABLE IF NOT EXISTS players (id int NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name tinytext)");
//			execute("CREATE TABLE IF NOT EXISTS looptime (looptime int NOT NULL DEFAULT 0, PRIMARY KEY(looptime), looptime2 int NOT NULL DEFAULT 0)");
//			execute("CREATE TABLE IF NOT EXISTS `player_stock_transactions` (`id` int(11) unsigned NOT NULL auto_increment,`player` tinytext NOT NULL,`stockID` tinytext NOT NULL,`trxn_type` enum('Buy','Sell') NOT NULL,`price` decimal(10,2) NOT NULL,`amount` int(11) NOT NULL, PRIMARY KEY  (`id`)) ENGINE=MyISAM  DEFAULT CHARSET=latin1 ;");	
//			execute("CREATE TABLE IF NOT EXISTS `stock_history` (`id` int(10) unsigned NOT NULL auto_increment,`stockID` tinytext NOT NULL,`price` decimal(10,2) NOT NULL,`change_amt` decimal(10,2) NOT NULL,`date_created` datetime NOT NULL,PRIMARY KEY  (`id`)) ENGINE=MyISAM  DEFAULT CHARSET=latin1;");
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//			ResultSet result = query("SELECT * FROM looptime");
//			
//			try {
//				boolean found = false;
//				while (result.next()) {
//					found = true;
//				}
//				if (!found) {
//					try {
//						execute("INSERT INTO looptime (looptime, looptime2) VALUES(0, 0)");
//					} catch (SQLException e) {
//						e.printStackTrace();
//					}	
//				}
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//			
//			try {
//				execute("ALTER TABLE stocks ADD COLUMN amount int");
//			} catch (SQLException e) {
//				// DO NOTHING
//			}
//			
//			try {
//				execute("ALTER TABLE stocks ADD COLUMN dividend decimal(10, 2)");
//			} catch (SQLException e) {
//				// DO NOTHING
//			}
	}
}