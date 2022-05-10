package ok.database;

import java.sql.*;
import java.util.*;

import ok.games.coingame.*;
import ok.games.coingame.tunnels.*;

public class CoinDB {
	
	private static final String PLAYERS_TABLE = "coingameplayers";
	private static final String COINS_TABLE = "coingamecoins";
	private static final String TUNNEL_NODES_TABLE = "coingametunnelnodes";
	private static final String TUNNEL_SEGMENTS_TABLE = "coingametunnelsegments";

	private Connection connection;

	private static final String createPlayersTable = 
			"CREATE TABLE IF NOT EXISTS " + PLAYERS_TABLE + " (\n"
			+ "	id SERIAL PRIMARY KEY,\n"
			+ "	numcoins integer NOT NULL DEFAULT 0,\n"
			+ "	x integer NOT NULL DEFAULT 0,\n"
			+ "	y integer NOT NULL DEFAULT 0,\n"
			+ " tunnelingLevel integer NOT NULL DEFAULT 0\n"
			+ " hat integer NOT NULL DEFAULT 0\n"
			+ ");";
	
	private static final String createCoinsTable = 
			"CREATE TABLE IF NOT EXISTS " + COINS_TABLE + " (\n"
			+ "	id SERIAL PRIMARY KEY,\n"
			+ "	x integer NOT NULL DEFAULT 0,\n"
			+ "	y integer NOT NULL DEFAULT 0,\n"
			+ " value integer NOT NULL DEFAULT 1\n"
			+ ");";
	
	private static final String createTunnelNodesTable = 
			"CREATE TABLE IF NOT EXISTS " + TUNNEL_NODES_TABLE + "(\n"
			+ " id SERIAL PRIMARY KEY,\n"
			+ " x integer NOT NULL,\n"
			+ " y integer NOT NULL,\n"
			+ " playerid integer NOT NULL\n"
			+ ");";

	private static final String createTunnelSegmentsTable = 
			"CREATE TABLE IF NOT EXISTS " + TUNNEL_SEGMENTS_TABLE + "(\n"
			+ " id SERIAL PRIMARY KEY,\n"
			+ " nodeid1 integer NOT NULL,\n"
			+ " nodeid2 integer NOT NULL,\n"
			+ " playerid integer NOT NULL\n"
			+ ");";
	
	private static final String createCoinsTable_addValueCol = 
			"ALTER TABLE " + COINS_TABLE + " ADD value integer NOT NULL DEFAULT 1;";
	private static final String createPlayersTable_addLevelCol = 
			"ALTER TABLE " + PLAYERS_TABLE + " ADD tunnelingLevel integer NOT NULL DEFAULT 0;";
	private static final String createPlayersTable_addHatCol = 
			"ALTER TABLE " + PLAYERS_TABLE + " ADD hat integer NOT NULL DEFAULT 0;";
	
	private static final String insertPlayerInfo = 
			"INSERT INTO " + PLAYERS_TABLE + "(id) VALUES(?)";
	private PreparedStatement insertPlayerInfoStatement;

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
//		createCoinGameTables();
		
		try {
			getPlayerInfoStatement = connection.prepareStatement(getPlayerInfo);
			insertPlayerInfoStatement = connection.prepareStatement(insertPlayerInfo);
			updatePlayerInfoStatement = connection.prepareStatement(updatePlayerInfo);
			
			insertCoinStatement = connection.prepareStatement(insertCoin, Statement.RETURN_GENERATED_KEYS);
			getCoinsStatement = connection.prepareStatement(getCoins);
			getCoinsInRangeStatement = connection.prepareStatement(getCoinsInRange);
			deleteCoinStatement = connection.prepareStatement(deleteCoin);
			getCoinStatement = connection.prepareStatement(getCoin);
			
			selectTunnelNodesStatement = connection.prepareStatement(selectTunnelNodes);
			selectTunnelSegmentsStatement = connection.prepareStatement(selectTunnelSegments);
			insertTunnelNodeStatement = connection.prepareStatement(insertTunnelNode);
			insertTunnelSegmentStatement = connection.prepareStatement(insertTunnelSegment);
			deleteTunnelSegmentStatement = connection.prepareStatement(deleteTunnelSegment);
			deleteTunnelNodeStatement = connection.prepareStatement(deleteTunnelNode);
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
		
		if (!DBUtil.doesTableHaveColumn(PLAYERS_TABLE, "tunnelingLevel")) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createPlayersTable_addLevelCol);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		if (!DBUtil.doesTableHaveColumn(PLAYERS_TABLE, "hat")) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createPlayersTable_addHatCol);
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

		if (!DBUtil.doesTableHaveColumn(COINS_TABLE, "value")) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createCoinsTable_addValueCol);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

//		if (!DBUtil.doesTableExist(TUNNELS_TABLE)) {
//			try (Statement stmt = connection.createStatement()) {
//				stmt.execute(createTunnelsTable);
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//		}

		if (!DBUtil.doesTableExist(TUNNEL_NODES_TABLE)) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createTunnelNodesTable);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (!DBUtil.doesTableExist(TUNNEL_SEGMENTS_TABLE)) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createTunnelSegmentsTable);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static final String getMaxIDFrom = "SELECT MAX(id) FROM ";
	private int getMaxIdFrom(String table) {
		try (Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(getMaxIDFrom + table)) {
			while (rs.next()) {
				int max = rs.getInt(1);
				System.err.println(max);
				return max;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	public int getMaxTunnelNodeId() {
		return getMaxIdFrom(TUNNEL_NODES_TABLE);
	}
	public int getMaxTunnelSegmentId() {
		return getMaxIdFrom(TUNNEL_SEGMENTS_TABLE);
	}

	private static final String getPlayerInfo = 
			"SELECT * FROM " + PLAYERS_TABLE + " WHERE id=?";
	private PreparedStatement getPlayerInfoStatement;
	public PlayerInfo getPlayerInfo(int id) {
		try {
			getPlayerInfoStatement.setInt(1, id);
			try (ResultSet rs = getPlayerInfoStatement.executeQuery()) {
				while (rs.next()) {
					return new PlayerInfo(
								rs.getInt("id"),
								rs.getInt("numcoins"),
								rs.getInt("x"),
								rs.getInt("y"),
								rs.getInt("tunnelingLevel"),
								rs.getInt("hat"));
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

	private static final String updatePlayerInfo = 
			"UPDATE " + PLAYERS_TABLE + " SET x=?, y=?, numcoins=?, tunnelingLevel=?, hat=? WHERE id=?";
	private PreparedStatement updatePlayerInfoStatement;
	public void updatePlayerInfo(PlayerInfo info) {
		try {
			updatePlayerInfoStatement.setInt(1, info.x);
			updatePlayerInfoStatement.setInt(2, info.y);
			updatePlayerInfoStatement.setInt(3, info.numcoins);
			updatePlayerInfoStatement.setInt(4, info.tunnelingExp);
			updatePlayerInfoStatement.setInt(5, info.hat);
			updatePlayerInfoStatement.setInt(6, info.id);
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
			DB.debug += "\n" + e.getMessage() + "\n";
		}
		DB.debug += "\n loaded " + list.size() + " coins \n";
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

	private static final String insertTunnelNode = 
			"INSERT INTO " + TUNNEL_NODES_TABLE + "(id, x, y, playerid) VALUES(?, ?, ?, ?)" +
			"ON CONFLICT (id) DO UPDATE SET x=EXCLUDED.x, y=EXCLUDED.y";
	private PreparedStatement insertTunnelNodeStatement;
	public void insertTunnelNode(TunnelNode node) {
		try {
			insertTunnelNodeStatement.setInt(1, node.id);
			insertTunnelNodeStatement.setInt(2, node.x);
			insertTunnelNodeStatement.setInt(3, node.y);
			insertTunnelNodeStatement.setInt(4, node.playerid);
			insertTunnelNodeStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static final String insertTunnelSegment = 
			"INSERT INTO " + TUNNEL_SEGMENTS_TABLE + "(id, nodeid1, nodeid2, playerid) VALUES(?, ?, ?, ?) ON CONFLICT DO NOTHING";
	private PreparedStatement insertTunnelSegmentStatement;
	public void insertTunnelSegment(TunnelSegment segment) {
		try {
			insertTunnelSegmentStatement.setInt(1, segment.id);
			insertTunnelSegmentStatement.setInt(2, segment.node1);
			insertTunnelSegmentStatement.setInt(3, segment.node2);
			insertTunnelSegmentStatement.setInt(4, segment.playerid);
			insertTunnelSegmentStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static final String deleteTunnelSegment = 
			"DELETE FROM " + TUNNEL_SEGMENTS_TABLE + " WHERE id=?;";
	private PreparedStatement deleteTunnelSegmentStatement;
	public void deleteTunnelSegment(TunnelSegment segment) {
		try {
			deleteTunnelSegmentStatement.setInt(1, segment.id);
			deleteTunnelSegmentStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static final String deleteTunnelNode = 
			"DELETE FROM " + TUNNEL_NODES_TABLE + " WHERE id=?;";
	private PreparedStatement deleteTunnelNodeStatement;
	public void deleteTunnelNode(TunnelNode node) {
		try {
			deleteTunnelNodeStatement.setInt(1, node.id);
			deleteTunnelNodeStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	

	private static final String selectTunnelNodes = 
			"SELECT * FROM " + TUNNEL_NODES_TABLE + " WHERE playerid=?";
	private PreparedStatement selectTunnelNodesStatement;
	private static final String selectTunnelSegments = 
			"SELECT * FROM " + TUNNEL_SEGMENTS_TABLE + " WHERE playerid=?";
	private PreparedStatement selectTunnelSegmentsStatement;
	public List<TunnelSegment> getTunnelsOfPlayer(int playerid, Set<TunnelNode> nodes) {
		List<TunnelSegment> segmentList = new ArrayList<>();
		try {
			// load tunnel nodes belonging to player
			selectTunnelNodesStatement.setInt(1, playerid);
			try (ResultSet rs = selectTunnelNodesStatement.executeQuery()) {
				while (rs.next()) {
					TunnelNode node = new TunnelNode(rs.getInt("id"), 
													rs.getInt("x"), 
													rs.getInt("y"), 
													playerid);
					nodes.add(node);
				}
			}
			// load tunnel segments belonging to player
			selectTunnelSegmentsStatement.setInt(1, playerid);
			try (ResultSet rs = selectTunnelSegmentsStatement.executeQuery()) {
				while (rs.next()) {
					int id = rs.getInt("id");
					int nodeid1 = rs.getInt("nodeid1");
					int nodeid2 = rs.getInt("nodeid2");
					TunnelSegment segment = new TunnelSegment(id, 
															nodeid1, 
															nodeid2,
															playerid);
					segmentList.add(segment);
				}
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return segmentList;
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
