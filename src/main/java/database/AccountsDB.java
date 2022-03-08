package database;

import java.sql.*;
import java.util.*;

import accounts.AccountInfo;

public class AccountsDB {

	private Connection connection;

	public AccountsDB(Connection connection) {
		this.connection = connection;
	}

	public void createAccountsTable() {
		String createtable = "CREATE TABLE IF NOT EXISTS accounts (\n"
				+ "	id integer PRIMARY KEY,\n"
				+ "	googleid text NOT NULL UNIQUE,\n"
				+ "	handle text NOT NULL\n"
				+ ");";
		try (Statement stmt = connection.createStatement()) {
			stmt.execute(createtable);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<AccountInfo> printAllAccounts() {
		String query = "SELECT * FROM accounts";
		List<AccountInfo> accounts = new ArrayList<>();
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query)) {
			while (rs.next()) {
				AccountInfo info = new AccountInfo(
						rs.getInt("id"),
						rs.getString("googleid"), 
						rs.getString("handle"));
				accounts.add(info);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return accounts;
	}
	
	public Optional<AccountInfo> query(int id) {
		String query = "SELECT * FROM accounts "
				+ "WHERE id=" + id;
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query)) {
			while (rs.next()) {
				return Optional.of(new AccountInfo(
						rs.getInt("id"),
						rs.getString("googleid"), 
						rs.getString("handle")));
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return Optional.empty();
	}
	
	public Optional<AccountInfo> query(String googleid) {
		String query = "SELECT * FROM accounts "
					+ "WHERE accounts.googleid='" + googleid + "'";
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query)) {
			while (rs.next()) {
				return Optional.of(new AccountInfo(
						rs.getInt("id"),
						rs.getString("googleid"), 
						rs.getString("handle")));
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return Optional.empty();
	}

	private static String INSERT = "INSERT INTO accounts(googleid, handle) VALUES(?,?)";
	public boolean insert(String googleid, String handle) {

		try (PreparedStatement pstmt = connection.prepareStatement(INSERT)) {
			pstmt.setString(1, googleid);
			pstmt.setString(2, handle);
			pstmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return false;
	}
	
}
