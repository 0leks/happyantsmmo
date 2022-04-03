package ok.database;

import java.sql.*;
import java.util.*;

public class DBUtil {
	
	enum DatabaseDriver {
		UNKNOWN, SQLITE, POSTGRES;
		
		public static DatabaseDriver fromDriverName(String driverName) {
			switch (driverName) {
			case "SQLite JDBC":
				return SQLITE;
			case "Postgres JDBC":
				return POSTGRES;
			default:
				return UNKNOWN;
			}
		}
	}

	private static Connection connection;
	private static DatabaseDriver driver;
	public static void setConnection(Connection c, String driverName) {
		connection = c;
		driver = DatabaseDriver.fromDriverName(driverName);
	}

	private static final String[] selectTables = {
			"",
			"SELECT name AS TABLE_NAME FROM sqlite_master WHERE type='table';",
			"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES;"
	};
	public static List<String> getTables() {
		List<String> tables = new LinkedList<>();
		if (connection == null) {
			return tables;
		}
		try (Statement stmt = connection.createStatement()) {
			try (ResultSet rs = stmt.executeQuery(selectTables[driver.ordinal()])) {
				while (rs.next()) {
					String tablename = rs.getString("TABLE_NAME");
					String[] names = {"accounts", "coingamecoins", "coingameplayers"};
					for (String name : names) {
						if(tablename.equals(name)) {
							tables.add(tablename);
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tables;
	}

	
	private static final String[] selectTableByName = {
			"",
			"SELECT name AS TABLE_NAME FROM sqlite_master WHERE type='table' AND TABLE_NAME=?;",
			"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?;"
	};
	private static PreparedStatement selectTableStatement;
	public static boolean doesTableExist(String tableName) {
		try {
			if (selectTableStatement == null) {
				selectTableStatement = connection.prepareStatement(selectTableByName[driver.ordinal()]);
			}
			System.err.println(selectTableByName[driver.ordinal()]);
			selectTableStatement.setString(1, tableName);
			try (ResultSet rs = selectTableStatement.executeQuery()) {
				while (rs.next()) {
					return true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
}
