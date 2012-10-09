package com.github.mashlol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBContext {
	private ConnectionManager readWriteConnections;
	private ConnectionManager readOnlyConnections;
	
	public DBContext () {
		readWriteConnections = new ConnectionManager(true);
		readOnlyConnections = new ConnectionManager(false);
	}
	
	public PreparedStatement PrepareStatementRead(String sql) {
		Connection con = readOnlyConnections.getConn();
		if (con == null) return null;
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement(sql);
		}
		catch (Throwable e) 
		{
			readOnlyConnections.freeConn(con);
			e.printStackTrace();
			con = null;
			ps = null;
		}
		return ps;
	}

	public PreparedStatement PrepareStatementWrite(String sql) {
		Connection con = readWriteConnections.getConn();
		if (con == null) return null;
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement(sql);
		}
		catch (Throwable e) 
		{
			readWriteConnections.freeConn(con);
			e.printStackTrace();
			con = null;
			ps = null;
		}
		return ps;
	}
	
	public boolean execute(PreparedStatement statm) {
		if (statm == null) return false;
		boolean res = false;
		Connection con = null;
		try {
			con = statm.getConnection();

			res = statm.execute();
			
			if (!res) {
				res = (statm.getUpdateCount() != 0);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				statm.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		
			try {
				if (con.isReadOnly()) {
					readOnlyConnections.freeConn(con);
				} else {
					readWriteConnections.freeConn(con);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return res;
	}

	public ResultSet executeQueryRead(String sql) {
		if (sql == null) return null;
		if (sql.isEmpty()) return null;
		PreparedStatement ps = null;
		ResultSet res = null;
		
		ps = PrepareStatementRead(sql);
		if (ps == null) return null;
		
		res = executeQuery(ps);

		return res;
	}

	public ResultSet executeQueryWrite(String sql) {
		if (sql == null) return null;
		if (sql.isEmpty()) return null;
		PreparedStatement ps = null;
		ResultSet res = null;
		
		ps = PrepareStatementWrite(sql);
		if (ps == null) return null;
		
		res = executeQuery(ps);

		return res;
	}

	public ResultSet executeQuery(PreparedStatement statm) {
		if (statm == null) return null;
		ResultSet res = null;
		Connection con = null;
		try {
			con = statm.getConnection();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		if (con == null) return null;
		try {
			res = statm.executeQuery();
		} 
		catch(Throwable e) 
		{
			e.printStackTrace();
			try {
				if (con.isReadOnly()) {
					readOnlyConnections.freeConn(con);
				} else {
					readWriteConnections.freeConn(con);
				}
			} catch (Throwable e1) {
			}
		}
		
		return res;
	}

	public int executeUpdate(String sql) {
		if (sql == null) return 0;
		if (sql.isEmpty()) return 0;
		PreparedStatement ps = null;
		int res = 0;
		
		ps = PrepareStatementRead(sql);
		if (ps == null) return -1;
		
		res = executeUpdate(ps);

		return res;
	}

	public int executeUpdate(PreparedStatement statm) {
		if (statm == null) return 0;
		int res = 0;
		Connection con = null;
		try {
			con = statm.getConnection();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return 0;
		}
		try {
			res = statm.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		} finally {
			try {
				statm.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		
			try {
				if (con.isReadOnly()) {
					readOnlyConnections.freeConn(con);
				} else {
					readWriteConnections.freeConn(con);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return res;
	}

	public void close(Statement statm) {
		if (statm == null) return;
		Connection con = null;
		try {
			con = statm.getConnection();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}
		if (con == null) return;
		
		try {
			statm.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			if (con.isReadOnly()) {
				readOnlyConnections.freeConn(con);
			} else {
				readWriteConnections.freeConn(con);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void close(ResultSet rset) {
		if (rset == null) return;
		
		Statement statm = null;
		
		try {
			statm = rset.getStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			rset.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (statm != null) {
			close(statm);
		}
	}
	
	public void close() {
		readWriteConnections.close();
		readOnlyConnections.close();
	}

//	private void setUpTables() {
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
//	}
}