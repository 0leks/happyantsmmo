package accounts;

import java.sql.*;
import java.util.*;

import app.GoogleAPI;

public class Accounts {
	
	private static final String DATA_DIR = "./data/";
	
	
	private static Connection dbcon;
	
	public static void connectToDB() {
		System.err.println("loading db");
		
		String url = "jdbc:sqlite:" + DATA_DIR + "accounts.db";
		
		try {
			dbcon = DriverManager.getConnection(url);
			if (dbcon != null) {
				DatabaseMetaData meta = dbcon.getMetaData();
				System.err.println("The driver name is " + meta.getDriverName());
				System.err.println("A new database has been created.");
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			System.err.println("FAILED TO OPEN ACCOUNTS DB, EXITING PROGRAM");
			System.exit(0);
		}
		createAccountsTable();
		
		
		while (printAllAccounts().size() < 3) {
			int id = (int)(Math.random()*100);
			insert("testaccount" + id, "TestHandle" + id);
		}
	}
	
	public static void createAccountsTable() {
		// SQL statement for creating a new table
		String createtable = "CREATE TABLE IF NOT EXISTS accounts (\n"
				+ "	id integer PRIMARY KEY,\n"
				+ "	googleid text NOT NULL,\n"
				+ "	handle text NOT NULL,\n"
				+ "	x integer NOT NULL DEFAULT 0,\n"
				+ "	y integer NOT NULL DEFAULT 0\n"
				+ ");";
		try (Statement stmt = dbcon.createStatement()) {
			stmt.execute(createtable);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	public static List<AccountInfo> printAllAccounts() {
		String query = "SELECT * FROM accounts";
//		String query = "SELECT id, googleid, handle, x, y FROM accounts";
		List<AccountInfo> accounts = new ArrayList<>();
		try (Statement stmt = dbcon.createStatement();
				ResultSet rs = stmt.executeQuery(query)) {
			// loop through the result set
			while (rs.next()) {
				AccountInfo info = new AccountInfo(
						rs.getInt("id"),
						rs.getString("googleid"), 
						rs.getString("handle"), 
						rs.getInt("x"), 
						rs.getInt("y"));
				accounts.add(info);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return accounts;
	}
	public static AccountInfo query(String googleid) {
		String query = "SELECT id, googleid, handle, x, y FROM accounts "
					+ "WHERE accounts.googleid='" + googleid + "'";
		try (Statement stmt = dbcon.createStatement();
				ResultSet rs = stmt.executeQuery(query)) {
			// loop through the result set
			while (rs.next()) {
				return new AccountInfo(
						rs.getInt("id"),
						rs.getString("googleid"), 
						rs.getString("handle"),
						rs.getInt("x"),
						rs.getInt("y"));
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	public static boolean insert(String googleid, String handle) {
		String sql = "INSERT INTO accounts(googleid, handle) VALUES(?,?)";

		try (PreparedStatement pstmt = dbcon.prepareStatement(sql)) {
			pstmt.setString(1, googleid);
			pstmt.setString(2, handle);
			pstmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return false;
	}
	
	public static void updateLocation(AccountInfo info) {
		String sql = "UPDATE accounts SET x = ?, y = ? WHERE id = ?";

		try (PreparedStatement pstmt = dbcon.prepareStatement(sql)) {
			pstmt.setInt(1, info.x);
			pstmt.setInt(2, info.y);
			pstmt.setInt(3, info.id);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static AccountInfo getAccountInfo(String token) {
		
		String id = GoogleAPI.getIdFromToken(token);
		if (id == null) {
			return null;
		}
		
		AccountInfo queried = query(id);
		
		if(queried != null) {
			return queried;
		}
		else {
			return new AccountInfo(0, id, null, 0, 0);
		}
	}
	
	public static boolean createAccount(String token, String handle) {
		String id = GoogleAPI.getIdFromToken(token);
		if (id == null) {
			return false;
		}
		
		return insert(id, handle);
	}
}
