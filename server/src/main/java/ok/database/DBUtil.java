package ok.database;

import java.sql.*;
import java.util.*;

public class DBUtil {

	private static Connection connection;
	public static void setConnection(Connection c) {
		connection = c;
	}

	public static List<String> getTables() {
		List<String> tables = new LinkedList<>();
		if (connection == null) {
			return tables;
		}
		String selectTables = "SELECT * FROM INFORMATION_SCHEMA.TABLES;";
		try (Statement stmt = connection.createStatement()) {
			try (ResultSet rs = stmt.executeQuery(selectTables)) {
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

	private static final String selectTable = 
			"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?;";
	private static PreparedStatement selectTableStatement;
	public static boolean doesTableExist(String tableName) {
		try {
			if (selectTableStatement == null) {
				selectTableStatement = connection.prepareStatement(selectTable);
			}
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
