package com.github.mashlol;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConnectionManager {
	private boolean readWriteConnections;
	
	public ConnectionManager() {
		this.readWriteConnections = false;
	}
	
	public ConnectionManager(boolean readWriteConnections) {
		this.readWriteConnections = readWriteConnections;
	}

	// Time between valid checks for busy connections
	private static final long CHECK_INTERVAL = 5*60*1000;
	private ConcurrentLinkedQueue<Connection> freeConnections = new ConcurrentLinkedQueue<Connection>();
	private ConcurrentLinkedQueue<Connection> busyConnections = new ConcurrentLinkedQueue<Connection>();
	private ConcurrentHashMap<Connection,Long> busyConnectionChecks = new ConcurrentHashMap<Connection,Long>();
	
	public Connection getConn() {
		// Check for any free connection
		Connection con = freeConnections.poll();

		// If connection is sane then use it
		try {
			if (con != null && !con.isClosed() && con.isValid(1)) return con;
		} catch (SQLException e1) {
		}

		cleanupBusyConnections();
		
		// If connection does exist, but is stale, then close it nicely and ignore errors
		try {
			if (con != null && !con.isClosed()) {
				try {
					con.close();
				} catch (SQLException e1) {
				}
			}
		} catch (SQLException e1) {
		}

		// At this point connection is not existent or dead so recreate
		try {
			con = createConnection();
		} catch (SQLException e) {
			// We can't create connections so print and get out
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			// We can't create connections so print and get out
			e.printStackTrace();
			return null;
		}

		// Check that creation succeeded
		try {
			if (con == null || con.isClosed()) {
				return null;
			}
		} catch (SQLException e) {
			// Created a closed connection somehow, print and get out
			e.printStackTrace();
			return null;
		}

		// Add connection to busy list
		busyConnections.add(con);
		
		return con;
	}
	
	public void freeConn(Connection con) {
		if (con == null) return;
		// Update out lists, we'll test connection sanity when we need it again
		if (busyConnections.remove(con)) {
			freeConnections.add(con);
		}
	}
	
	protected void cleanupBusyConnections() {
		// Leave if no nothing to do
		if (busyConnections.size() == 0) return;
		
		List<Connection> staleConnections = new ArrayList<Connection>();
		List<Connection> releaseConnections = new ArrayList<Connection>();
		
		long currMillis = System.currentTimeMillis();
		for(Connection c : busyConnections) {
			try {
				if (c.isClosed()) {
					staleConnections.add(c);
					continue;
				}
			} catch (SQLException e) {
				e.printStackTrace();
				staleConnections.add(c);
				continue;
			}
			Long lastCheck = busyConnectionChecks.get(c);
			if (lastCheck == null || (currMillis-lastCheck)/1000 > CHECK_INTERVAL) {
				busyConnectionChecks.put(c, currMillis);
				try {
					if (!c.isValid(1)) {
						staleConnections.add(c);
						continue;
					} else {
						releaseConnections.add(c);
						continue;
					}
				} catch (SQLException e) {
					e.printStackTrace();
					staleConnections.add(c);
					continue;
				}
			}
		}
		
		// Any connections we can release back to free pool ?
		if (releaseConnections.size() > 0) {
			for(Connection c : releaseConnections) {
				busyConnections.remove(c);
				busyConnectionChecks.remove(c);

				freeConnections.add(c);
			}
		}
		
		// Any dead or stale connections we need to remove ?
		if (staleConnections.size() > 0) {
			for(Connection c : staleConnections) {
				busyConnections.remove(c);
				busyConnectionChecks.remove(c);
				
				try {
					c.close();
				} catch (SQLException e) {
					// We don't care about the result we knew the connection was bad already
				}
			}
		}
	}
	
	protected Connection createConnection() throws SQLException, ClassNotFoundException {
		String driver = "com.mysql.jdbc.Driver";
		String connection = "jdbc:mysql://" + StockMarket.mysqlIP + ":" + StockMarket.mysqlPort + "/" + StockMarket.mysqlDB;
		String user = StockMarket.mysqlUser;
		String password = StockMarket.mysqlPW;
		Connection con = null;
		
		try {
			Class.forName(driver);
			con = DriverManager.getConnection(connection, user, password);
			if (!readWriteConnections) {
				con.setReadOnly(false);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw e;
		}
		
		return con;
	}

	public void close() {
		// Shutdown all connections by force and empty queues
		if (freeConnections != null && freeConnections.size() > 0) {
			Connection c;
			do {
				c = freeConnections.poll();
				if (c != null) {
					try {
						c.close();
					} catch (Throwable e) {
					}
				}
			} while (c != null);
		}
		if (busyConnections != null && busyConnections.size() > 0) {
			Connection c;
			do {
				c = busyConnections.poll();
				if (c != null) {
					try {
						c.close();
					} catch (Throwable e) {
					}
				}
			} while (c != null);
		}
	}
}
