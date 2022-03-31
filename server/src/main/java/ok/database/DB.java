package ok.database;

import java.sql.*;
import java.util.*;

import ok.games.coingame.PlayerInfo;

public class DB {
//	String url = "jdbc:sqlite:./data/main.db";
	
	public static String debug = "";
	
	private static String connectionString = System.getenv("DBSTRING");
	private static Connection connection;
	public static AccountsDB accountsDB;
	public static CoinDB coinsDB;
	
	public static String getConnectionString() {
		return connectionString;
	}
	public static boolean isConnected() {
		return connection != null;
	}

	static {
		String url = getConnectionString();
		if (url == null) {
			System.err.println("No ConnectionString");
			debug += "No ConnectionString\n";
		}
		else {
			try {
				System.err.println(url);
				connection = DriverManager.getConnection(url);
				if (connection != null) {
					DatabaseMetaData meta = connection.getMetaData();
					System.err.println("The driver name is " + meta.getDriverName());
					debug += "The driver name is " + meta.getDriverName() + "\n";
				}
				
				DBUtil.setConnection(connection);
				
				accountsDB = new AccountsDB(connection);
				coinsDB = new CoinDB(connection);
				accountsDB.createAccountsTable();
				coinsDB.createCoinGameTables();
			} catch (SQLException e) {
				System.err.println(e.getMessage());
				debug += e.getMessage() + "\n";
				System.err.println("FAILED TO OPEN DB");
			}
				
		}
	}
}
