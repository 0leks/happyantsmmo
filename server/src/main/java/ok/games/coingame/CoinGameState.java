package ok.games.coingame;

import java.util.*;
import java.util.concurrent.*;

import ok.database.DB;
import ok.games.coingame.tunnels.*;
import ok.games.math.Vec2;

public class CoinGameState {

	private Map<Integer, Coin> loadedCoins = new ConcurrentHashMap<>();
	private Map<Integer, PlayerInfo> loadedPlayerInfo = new ConcurrentHashMap<>();
	private Map<Integer, TunnelNode> loadedTunnelNodes = new ConcurrentHashMap<>();
	
	private ConcurrentLinkedQueue<Coin> collectedCoins = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<Coin> newCoins = new ConcurrentLinkedQueue<>();
	
	private ConcurrentLinkedQueue<TunnelNode> newTunnelNodes = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<TunnelSegment> newTunnelSegments = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<TunnelSegment> deleteTunnelSegments = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<TunnelNode> deleteTunnelNodes = new ConcurrentLinkedQueue<>();
	private Map<Integer, Map<Integer, TunnelSegment>> playerToTunnels = new HashMap<>();
	
	private volatile int maxCoinID;
	private volatile int maxTunnelNodeID;
	private volatile int maxTunnelSegmentID;
	private boolean autoWrite;
	
	public CoinGameState(boolean autoWrite) {
		this.autoWrite = autoWrite;
		for(Coin coin : DB.coinsDB.getCoins()) {
			loadedCoins.put(coin.id, coin);
			maxCoinID = Math.max(maxCoinID, coin.id);
		}
		maxTunnelNodeID = DB.coinsDB.getMaxTunnelNodeId();
		maxTunnelSegmentID = DB.coinsDB.getMaxTunnelSegmentId();
		System.out.println("Loaded " + loadedCoins.size() + " coins from DB");
	}
	
	public void persist() {
		for (PlayerInfo player : loadedPlayerInfo.values()) {
			DB.coinsDB.updatePlayerInfo(player);
		}
		System.out.println("Saved " + loadedPlayerInfo.size() + " players to DB");
		int newCoinsCount = 0;
		while (!newCoins.isEmpty()) {
			Coin coin = newCoins.remove();
			DB.coinsDB.insertCoin(coin);
			++newCoinsCount;
		}
		System.out.println("Saved " + newCoinsCount + " new coins to DB");
		
		int removedCoinsCount = 0;
		while (!collectedCoins.isEmpty()) {
			Coin coin = collectedCoins.remove();
			DB.coinsDB.deleteCoin(coin);
			++removedCoinsCount;
		}
		System.out.println("Saved " + removedCoinsCount + " removed coins to DB");
		
		int newTunnelNodesCount = 0;
		while (!newTunnelNodes.isEmpty()) {
			TunnelNode node = newTunnelNodes.remove();
			DB.coinsDB.insertTunnelNode(node);
			++newTunnelNodesCount;
		}
		System.out.println("Saved " + newTunnelNodesCount + " new tunnel nodes to DB");

		int newTunnelSegmentsCount = 0;
		while (!newTunnelSegments.isEmpty()) {
			TunnelSegment segment = newTunnelSegments.remove();
			DB.coinsDB.insertTunnelSegment(segment);
			++newTunnelSegmentsCount;
		}
		System.out.println("Saved " + newTunnelSegmentsCount + " new tunnel segments to DB");
		
		int collapsedTunnelSegmentsCount = 0;
		while (!deleteTunnelSegments.isEmpty()) {
			TunnelSegment segment = deleteTunnelSegments.remove();
			DB.coinsDB.deleteTunnelSegment(segment);
			++collapsedTunnelSegmentsCount;
		}
		System.out.println("Saved " + collapsedTunnelSegmentsCount + " collapsed tunnel segments to DB");

		int collapsedTunnelNodesCount = 0;
		while (!deleteTunnelNodes.isEmpty()) {
			TunnelNode node = deleteTunnelNodes.remove();
			DB.coinsDB.deleteTunnelNode(node);
			++collapsedTunnelNodesCount;
		}
		System.out.println("Saved " + collapsedTunnelNodesCount + " collapsed tunnel nodes to DB");
	}
	
	public PlayerInfo getPlayerInfo(int id) {
		if (!loadedPlayerInfo.containsKey(id)) {
			PlayerInfo info = DB.coinsDB.getPlayerInfo(id);
			if(info == null) {
				return info;
			}
			loadedPlayerInfo.put(id, info);
		}
		return loadedPlayerInfo.get(id);
	}
	
	public PlayerInfo createNewPlayer(int id) {
		// if player already exists, simply return it
		if (loadedPlayerInfo.containsKey(id) && loadedPlayerInfo.get(id) != null) {
			return loadedPlayerInfo.get(id);
		}
		PlayerInfo playerInfo = DB.coinsDB.insertPlayerInfo(id);
		loadedPlayerInfo.put(id, playerInfo);
		return playerInfo;
	}
	
	public void updatePlayerInfo(PlayerInfo info) {
		loadedPlayerInfo.put(info.id, info);
		if (autoWrite) {
			DB.coinsDB.updatePlayerInfo(info);
		}
	}
	
	public Coin addNewCoin(int x, int y, int value) {
		Coin newcoin = new Coin(++maxCoinID, x, y, value);
		loadedCoins.put(newcoin.id, newcoin);
		newCoins.add(newcoin);
		if (autoWrite) {
			DB.coinsDB.insertCoin(newcoin);
		}
		return newcoin;
	}
	
	public Collection<Coin> getCoins() {
		return loadedCoins.values();
	}
	
	public List<Coin> getCoinsInRange(Vec2 min, Vec2 max) {
		List<Coin> inrange = new LinkedList<>();
		for(Coin coin : loadedCoins.values()) {
			if(min.x <= coin.x && coin.x <= max.x 
					&& min.y <= coin.y && coin.y <= max.y) {
				inrange.add(coin);
			}
		}
		return inrange;
	}
	
	public void playerCollectsCoin(PlayerInfo player, Coin coin) {
		loadedCoins.remove(coin.id);
		collectedCoins.add(coin);
		player.numcoins += coin.value;
		updatePlayerInfo(player);
		
		if (autoWrite) {
			DB.coinsDB.deleteCoin(coin);
		}
	}
	
	public void playerUnlocksTunneling(PlayerInfo player) {
		if (player.tunnelingExp == 0 && player.numcoins >= Constants.UNLOCK_TUNNELING_COST) {
			player.numcoins -= 1000;
			player.tunnelingExp = 1;
			updatePlayerInfo(player);
		}
	}
	public void playerPurchasesItem(PlayerInfo player, String item) {
		if (player.numcoins >= Constants.CROWN_COST) {
			player.numcoins -= Constants.CROWN_COST;
			player.hat = 1;
			updatePlayerInfo(player);
		}
	}
	
	public void updateTunnelSegment(TunnelSegment segment) {
		newTunnelSegments.add(segment);
	}
	
	public TunnelNode updateTunnelNodePosition(int nodeid, Vec2 newPosition) {
		TunnelNode node = loadedTunnelNodes.get(nodeid);
		node.x = newPosition.x;
		node.y = newPosition.y;
		newTunnelNodes.add(node);
		return node;
	}
	
	public TunnelNode createNewTunnelNode(int x, int y, int playerid) {
		TunnelNode node = new TunnelNode(++maxTunnelNodeID, 
										x, 
										y, 
										playerid);
		newTunnelNodes.add(node);
		loadedTunnelNodes.put(node.id, node);
		return node;
	}
	
	public void playerGainsExp(PlayerInfo player, int exp) {
		player.tunnelingExp += exp;
		updatePlayerInfo(player);
	}
	
	public TunnelSegment createNewTunnelSegment(int nodeid1, int nodeid2, int playerid) {
		TunnelSegment segment = new TunnelSegment(++maxTunnelSegmentID, 
												nodeid1, 
												nodeid2,
												playerid);
		newTunnelSegments.add(segment);
		playerToTunnels.get(playerid).put(segment.id, segment);
		
//		double length = getTunnelNodePosition(nodeid1).distanceTo(getTunnelNodePosition(nodeid2));
//		loadedPlayerInfo.get(playerid).tunnelingExp += 10 + length/100;
//		updatePlayerInfo(loadedPlayerInfo.get(playerid));
		
		return segment;
	}
	
	public TunnelSegment collapseTunnelSegment(int nodeid1, int nodeid2, int playerid) {
		for (TunnelSegment segment : playerToTunnels.get(playerid).values()) {
			if ((segment.node1 == nodeid1 && segment.node2 == nodeid2)
					|| (segment.node1 == nodeid2 && segment.node2 == nodeid1)) {
				return collapseTunnelSegment(segment.id, playerid);
//				deleteTunnelSegments.add(segment);
//				playerToTunnels.get(playerid).remove(segment.id);
//				
//				double length = getTunnelNodePosition(nodeid1).distanceTo(getTunnelNodePosition(nodeid2));
//				loadedPlayerInfo.get(playerid).tunnelingExp += 1 + length/200;
//				updatePlayerInfo(loadedPlayerInfo.get(playerid));
//				
//				return segment;
			}
		}
		return null;
	}
	
	public TunnelSegment collapseTunnelSegment(int segmentid, int playerid) {
		TunnelSegment segment = playerToTunnels.get(playerid).get(segmentid);
		deleteTunnelSegments.add(segment);
		playerToTunnels.get(playerid).remove(segment.id);
		
//		double length = getTunnelNodePosition(segment.node1).distanceTo(getTunnelNodePosition(segment.node2));
//		loadedPlayerInfo.get(playerid).tunnelingExp += 1 + length/200;
//		updatePlayerInfo(loadedPlayerInfo.get(playerid));
		
		return segment;
	}
	
	public void deleteNode(int nodeid) {
		deleteTunnelNodes.add(loadedTunnelNodes.get(nodeid));
	}
	
	public boolean doesTunnelNodeExist(int nodeid) {
		return loadedTunnelNodes.containsKey(nodeid);
	}
	
	public TunnelNode getTunnelNode(int nodeid) {
		return loadedTunnelNodes.get(nodeid);
	}
	
	public Set<TunnelSegment> getTunnelSegmentFrom(int nodeid1, int playerid) {
		Set<TunnelSegment> segments = new HashSet<>();
		for (TunnelSegment segment : playerToTunnels.get(playerid).values()) {
			if (segment.node1 == nodeid1 || segment.node2 == nodeid1) {
				segments.add(segment);
			}
		}
		return segments;
	}
	
	public Collection<TunnelSegment> getPlayerTunnels(int playerid) {
		return playerToTunnels.get(playerid).values();
	}
	
	public Vec2 getTunnelNodePosition(int nodeid) {
		TunnelNode node = loadedTunnelNodes.get(nodeid);
		return new Vec2(node.x, node.y);
	}
	
	public List<TunnelSegment> getTunnelsOfPlayer(int playerid, Set<TunnelNode> nodes) {
		if (!playerToTunnels.containsKey(playerid)) {
			playerToTunnels.put(playerid, new ConcurrentHashMap<>());
		}
		Map<Integer, TunnelSegment> playerTunnels = playerToTunnels.get(playerid);
		Set<TunnelNode> playerNodes = new HashSet<>();
		List<TunnelSegment> tunnelSegments = DB.coinsDB.getTunnelsOfPlayer(playerid, playerNodes);

		// Dont override in-mem values
		for (TunnelNode node : playerNodes) {
			if (!loadedTunnelNodes.containsKey(node.id)) {
				loadedTunnelNodes.put(node.id, node);
			}
		}

		// Dont override in-mem values
		for (TunnelSegment segment : tunnelSegments) {
			if (!playerTunnels.containsKey(segment.id)) {
				playerTunnels.put(segment.id, segment);
			}
			else {
				segment = playerTunnels.get(segment.id);
			}
			nodes.add(loadedTunnelNodes.get(segment.node1));
			nodes.add(loadedTunnelNodes.get(segment.node2));
		}
		return tunnelSegments;
	}
}
