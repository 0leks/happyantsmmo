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
	

	public CoinDB(Connection connection) {
		this.connection = connection;
		createCoinGameTables();
		
		try {
			getPlayerInfoStatement = connection.prepareStatement(getPlayerInfo);
			insertPlayerInfoStatement = connection.prepareStatement(insertPlayerInfo);
			updateLocationStatement = connection.prepareStatement(updateLocation);
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

	public List<Coin> getCoins() {
		return null;
	}
}
