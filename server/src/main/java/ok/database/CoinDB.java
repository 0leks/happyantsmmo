package ok.database;

import java.sql.*;
import java.util.*;

import ok.games.coingame.*;

public class CoinDB {
	
	private static final String PLAYERS_TABLE = "coingameplayers";
	private static final String COINS_TABLE = "coingamecoins";
	private static final String TUNNELS_TABLE = "coingametunnels";

	private Connection connection;

	private static final String createPlayersTable = 
			"CREATE TABLE IF NOT EXISTS " + PLAYERS_TABLE + " (\n"
			+ "	id SERIAL PRIMARY KEY,\n"
			+ "	numcoins integer NOT NULL DEFAULT 0,\n"
			+ "	x integer NOT NULL DEFAULT 0,\n"
			+ "	y integer NOT NULL DEFAULT 0\n"
			+ ");";
	
	private static final String createCoinsTable = 
			"CREATE TABLE IF NOT EXISTS " + COINS_TABLE + " (\n"
			+ "	id SERIAL PRIMARY KEY,\n"
			+ "	x integer NOT NULL DEFAULT 0,\n"
			+ "	y integer NOT NULL DEFAULT 0,\n"
			+ " value integer NOT NULL DEFAULT 1\n"
			+ ");";

	private static final String createTunnelsTable = 
			"CREATE TABLE IF NOT EXISTS " + TUNNELS_TABLE + " (\n"
			+ "	id SERIAL PRIMARY KEY,\n"
			+ "	x1 integer NOT NULL DEFAULT 0,\n"
			+ "	y1 integer NOT NULL DEFAULT 0,\n"
			+ "	x2 integer NOT NULL DEFAULT 0,\n"
			+ "	y2 integer NOT NULL DEFAULT 0,\n"
			+ " playerid integer NOT NULL\n"
			+ ");";
	
	private static final String createCoinsTable_addValueCol = 
			"ALTER TABLE " + COINS_TABLE + " ADD value integer NOT NULL DEFAULT 1;";
	
	private static final String getPlayerInfo = 
			"SELECT * FROM " + PLAYERS_TABLE + " WHERE id=?";
	private PreparedStatement getPlayerInfoStatement;
	
	private static final String insertPlayerInfo = 
			"INSERT INTO " + PLAYERS_TABLE + "(id) VALUES(?)";
	private PreparedStatement insertPlayerInfoStatement;
	
	private static final String updatePlayerInfo = 
			"UPDATE " + PLAYERS_TABLE + " SET x=?, y=?, numcoins=? WHERE id=?";
	private PreparedStatement updatePlayerInfoStatement;

	private static final String insertCoin = 
			"INSERT INTO " + COINS_TABLE + "(id, x, y, value) VALUES(?, ?, ?, ?) ON CONFLICT DO NOTHING";
	private PreparedStatement insertCoinStatement;

	private static final String getCoins = 
			"SELECT * FROM " + COINS_TABLE;
	private PreparedStatement getCoinsStatement;

	private static final String getCoin = 
			"SELECT * FROM " + COINS_TABLE + " WHERE id=?";
	private PreparedStatement getCoinStatement;
	
	private static final String getCoinsInRange = 
			"SELECT * FROM " + COINS_TABLE + " WHERE (x BETWEEN ? AND ?) AND (y BETWEEN ? AND ?)";
	private PreparedStatement getCoinsInRangeStatement;

	private static final String deleteCoin = 
			"DELETE FROM " + COINS_TABLE + " WHERE id=?";
	private PreparedStatement deleteCoinStatement;
	
//	private static final String incrementNumcoins =
//			"UPDATE " + PLAYERS_TABLE + " SET numcoins=numcoins+1 WHERE id=?";
//	private PreparedStatement incrementNumcoinsStatement;

	public CoinDB(Connection connection) {
		this.connection = connection;
		createCoinGameTables();
		
		try {
			getPlayerInfoStatement = connection.prepareStatement(getPlayerInfo);
			insertPlayerInfoStatement = connection.prepareStatement(insertPlayerInfo);
			updatePlayerInfoStatement = connection.prepareStatement(updatePlayerInfo);
			insertCoinStatement = connection.prepareStatement(insertCoin, Statement.RETURN_GENERATED_KEYS);
			getCoinsStatement = connection.prepareStatement(getCoins);
			getCoinsInRangeStatement = connection.prepareStatement(getCoinsInRange);
			deleteCoinStatement = connection.prepareStatement(deleteCoin);
			getCoinStatement = connection.prepareStatement(getCoin);
			
			insertTunnelStatement = connection.prepareStatement(insertTunnel);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public void createCoinGameTables() {
		if (!DBUtil.doesTableExist(PLAYERS_TABLE)) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createPlayersTable);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (!DBUtil.doesTableExist(COINS_TABLE)) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createCoinsTable);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (!DBUtil.doesTableExist(TUNNELS_TABLE)) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createTunnelsTable);
			} catch (SQLException e) {
				e.printStackTrace();
			}
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

	public void updatePlayerInfo(PlayerInfo info) {
		try {
			updatePlayerInfoStatement.setInt(1, info.x);
			updatePlayerInfoStatement.setInt(2, info.y);
			updatePlayerInfoStatement.setInt(3, info.numcoins);
			updatePlayerInfoStatement.setInt(4, info.id);
			updatePlayerInfoStatement.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void insertCoin(Coin coin) {
		try {
			insertCoinStatement.setInt(1, coin.id);
			insertCoinStatement.setInt(2, coin.x);
			insertCoinStatement.setInt(3, coin.y);
			insertCoinStatement.setInt(4, coin.value);
			insertCoinStatement.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public Coin getCoin(int id) {
		try {
			getCoinStatement.setInt(1, id);
			try (ResultSet rs = getCoinStatement.executeQuery()) {
				while (rs.next()) {
					return new Coin(
							rs.getInt("id"),
							rs.getInt("x"),
							rs.getInt("y"),
							rs.getInt("value"));
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	public List<Coin> getCoins() {
		List<Coin> list = new ArrayList<>();
		try {
			try (ResultSet rs = getCoinsStatement.executeQuery()) {
				while (rs.next()) {
					list.add(new Coin(
							rs.getInt("id"),
							rs.getInt("x"),
							rs.getInt("y"),
							rs.getInt("value")));
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
							rs.getInt("y"),
							rs.getInt("value")));
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return list;
	}
	
	public void deleteCoin(Coin coin) {
		try {
			deleteCoinStatement.setInt(1, coin.id);
			deleteCoinStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static final String insertTunnel = 
			"INSERT INTO " + TUNNELS_TABLE + "(id, x1, y1, x2, y2, playerid) VALUES(?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";
	private PreparedStatement insertTunnelStatement;
	public void insertTunnel(Tunnel tunnel) {
		try {
			insertTunnelStatement.setInt(1, tunnel.id);
			insertTunnelStatement.setInt(2, tunnel.x1);
			insertTunnelStatement.setInt(3, tunnel.y1);
			insertTunnelStatement.setInt(4, tunnel.x2);
			insertTunnelStatement.setInt(5, tunnel.y2);
			insertTunnelStatement.setInt(6, tunnel.playerid);
			insertTunnelStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
//	public void collected(int playerid, int coinid) {
//		
//		try {
//			connection.setAutoCommit(false);
//			deleteCoinStatement.setInt(1, coinid);
//			deleteCoinStatement.execute();
//			
//			incrementNumcoinsStatement.setInt(1, playerid);
//			incrementNumcoinsStatement.execute();
//		} catch (SQLException e) {
//			try {
//				connection.rollback();
//			} catch (SQLException e1) {
//				e1.printStackTrace();
//			}
//			e.printStackTrace();
//		}
//		finally {
//			try {
//				connection.commit();
//				connection.setAutoCommit(true);
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//		}
//		
//	}
}
