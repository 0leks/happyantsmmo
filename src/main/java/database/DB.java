package database;

import java.sql.*;

public class DB {
	private static final String DATA_DIR = "./data/";
	
	private static Connection connection;

	public static final AccountsDB accountsDB;
	public static final CoinDB coinsDB;
	
	static {
		String url = "jdbc:sqlite:" + DATA_DIR + "main.db";
		try {
			connection = DriverManager.getConnection(url);
			if (connection != null) {
				DatabaseMetaData meta = connection.getMetaData();
				System.err.println("The driver name is " + meta.getDriverName());
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			System.err.println("FAILED TO OPEN DB, EXITING PROGRAM");
			System.exit(0);
		}
			
		accountsDB = new AccountsDB(connection);
		coinsDB = new CoinDB(connection);

		accountsDB.createAccountsTable();
		coinsDB.createCoinGameTables();
		
	}
}
