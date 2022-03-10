package database;

import java.sql.*;
import java.util.*;

import games.coingame.*;

public class CoinDB {
	
	private static final String PLAYERS_TABLE = "coingameplayers";
	private static final String COINS_TABLE = "coingamecoins";

	private Connection connection;
	

	private static final String createtable = 
			"CREATE TABLE IF NOT EXISTS " + PLAYERS_TABLE + " (\n"
			+ "	id integer PRIMARY KEY,\n"
			+ "	numcoins integer NOT NULL DEFAULT 0,\n"
			+ "	x integer NOT NULL DEFAULT 0,\n"
			+ "	y integer NOT NULL DEFAULT 0\n"
			+ ");";
	
	private static final String createtable2 = 
			"CREATE TABLE IF NOT EXISTS " + COINS_TABLE + " (\n"
			+ "	id integer PRIMARY KEY,\n"
			+ "	x integer NOT NULL DEFAULT 0,\n"
			+ "	y integer NOT NULL DEFAULT 0\n"
			+ ");";
	
	private static final String getPlayerInfo = 
			"SELECT * FROM " + PLAYERS_TABLE + " WHERE id=?";
	private PreparedStatement getPlayerInfoStatement;
	
	private static final String insertPlayerInfo = 
			"INSERT INTO " + PLAYERS_TABLE + "(id) VALUES(?)";
	private PreparedStatement insertPlayerInfoStatement;
	
	private static final String updateLocation = 
			"UPDATE " + PLAYERS_TABLE + " SET x=?, y=? WHERE id=?";
	private PreparedStatement updateLocationStatement;

	private static final String insertCoin = 
			"INSERT INTO " + COINS_TABLE + "(x, y) VALUES(?, ?)";
	private PreparedStatement insertCoinStatement;

	private static final String getCoins = 
			"SELECT * FROM " + COINS_TABLE;
	private PreparedStatement getCoinsStatement;
	
	private static final String getCoinsInRange = 
			"SELECT * FROM " + COINS_TABLE + " WHERE (x BETWEEN ? AND ?) AND (y BETWEEN ? AND ?)";
	private PreparedStatement getCoinsInRangeStatement;

	private static final String deleteCoin = 
			"DELETE FROM " + COINS_TABLE + " WHERE id=?";
	private PreparedStatement deleteCoinStatement;
	
	private static final String incrementNumcoins =
			"UPDATE " + PLAYERS_TABLE + " SET numcoins=numcoins+1 WHERE id=?";
	private PreparedStatement incrementNumcoinsStatement;

	public CoinDB(Connection connection) {
		this.connection = connection;
		createCoinGameTables();
		
		try {
			getPlayerInfoStatement = connection.prepareStatement(getPlayerInfo);
			insertPlayerInfoStatement = connection.prepareStatement(insertPlayerInfo);
			updateLocationStatement = connection.prepareStatement(updateLocation);
			insertCoinStatement = connection.prepareStatement(insertCoin);
			getCoinsStatement = connection.prepareStatement(getCoins);
			getCoinsInRangeStatement = connection.prepareStatement(getCoinsInRange);
			deleteCoinStatement = connection.prepareStatement(deleteCoin);
			incrementNumcoinsStatement = connection.prepareStatement(incrementNumcoins);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public void createCoinGameTables() {
		try (Statement stmt = connection.createStatement()) {
			stmt.execute(createtable);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		try (Statement stmt = connection.createStatement()) {
			stmt.execute(createtable2);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public PlayerInfo getPlayerInfo(int id) {
		try {
			getPlayerInfoStatement.setInt(1, id);
			try (ResultSet rs = getPlayerInfoStatement.executeQuery()) {
				while (rs.next()) {
					return new PlayerInfo(
								rs.getInt("id"),
								rs.getInt("numcoins"),
								rs.getInt("x"),
								rs.getInt("y"));
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	public PlayerInfo insertPlayerInfo(int id) {
		try {
			insertPlayerInfoStatement.setInt(1, id);
			insertPlayerInfoStatement.executeUpdate();
			return getPlayerInfo(id);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	public void updateLocation(PlayerInfo info) {
		try {
			updateLocationStatement.setInt(1, info.x);
			updateLocationStatement.setInt(2, info.y);
			updateLocationStatement.setInt(3, info.id);
			updateLocationStatement.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void insertCoin(int x, int y) {
		try {
			insertCoinStatement.setInt(1, x);
			insertCoinStatement.setInt(2, y);
			insertCoinStatement.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public List<Coin> getCoins() {
		List<Coin> list = new ArrayList<>();
		try {
			try (ResultSet rs = getCoinsStatement.executeQuery()) {
				while (rs.next()) {
					list.add(new Coin(
							rs.getInt("id"),
							rs.getInt("x"),
							rs.getInt("y")));
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return list;
	}
	
	public List<Coin> getCoinsInRange(int minx, int miny, int maxx, int maxy) {
		List<Coin> list = new ArrayList<>();
		try {
			getCoinsInRangeStatement.setInt(1, minx);
			getCoinsInRangeStatement.setInt(2, maxx);
			getCoinsInRangeStatement.setInt(3, miny);
			getCoinsInRangeStatement.setInt(4, maxy);
			try (ResultSet rs = getCoinsInRangeStatement.executeQuery()) {
				while (rs.next()) {
					list.add(new Coin(
							rs.getInt("id"),
							rs.getInt("x"),
							rs.getInt("y")));
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return list;
	}
	
	public void collected(int playerid, int coinid) {
		
		try {
			connection.setAutoCommit(false);
			deleteCoinStatement.setInt(1, coinid);
			deleteCoinStatement.execute();
			
			incrementNumcoinsStatement.setInt(1, playerid);
			incrementNumcoinsStatement.execute();
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		finally {
			try {
				connection.commit();
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}
}
