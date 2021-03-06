package ok.database;

import java.sql.*;
import java.util.*;

import ok.accounts.AccountInfo;

public class AccountsDB {

	private Connection connection;

	public AccountsDB(Connection connection) {
		this.connection = connection;
	}

	public void createAccountsTable() {
		if (DBUtil.doesTableExist("accounts")) {
			return;
		}
		String createtable = "CREATE TABLE IF NOT EXISTS accounts (\n"
				+ "	id SERIAL PRIMARY KEY,\n"
				+ "	googleid text NOT NULL UNIQUE,\n" // TODO index on googleid for quick lookup
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

	/**
	 * @return AccountInfo on success, 
	 * 			empty optional on nonexistant account
	 */
	public Optional<AccountInfo> query(int id) throws SQLException {
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
	
	/**
	 * @return AccountInfo on success, 
	 * 			empty optional on nonexistant account
	 */
	public Optional<AccountInfo> query(String googleid) throws SQLException {
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
	
	private static String DELETE = "DELETE FROM accounts WHERE id = ?";
	public boolean delete(int accountID) {

		try (PreparedStatement pstmt = connection.prepareStatement(DELETE)) {
			pstmt.setInt(1, accountID);
			pstmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return false;
	}
	
}
