package ok.games.coingame;

import java.util.*;
import java.util.concurrent.*;

import ok.database.DB;
import ok.games.math.Vec2;

public class CoinGameState {

	private Map<Integer, Coin> loadedCoins = new ConcurrentHashMap<>();
	private Map<Integer, PlayerInfo> loadedPlayerInfo = new ConcurrentHashMap<>();
	private ConcurrentLinkedQueue<Coin> collectedCoins = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<Coin> newCoins = new ConcurrentLinkedQueue<>();
	
	private int maxCoinID;
	private boolean autoWrite;
	
	public CoinGameState(boolean autoWrite) {
		this.autoWrite = autoWrite;
		for(Coin coin : DB.coinsDB.getCoins()) {
			loadedCoins.put(coin.id, coin);
			maxCoinID = Math.max(maxCoinID, coin.id);
		}
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
	
	public void updatePlayerLocation(PlayerInfo info) {
		loadedPlayerInfo.put(info.id, info);
		if (autoWrite) {
			DB.coinsDB.updatePlayerInfo(info);
		}
	}
	
	public void addNewCoin(int x, int y, int value) {
		Coin newcoin = new Coin(++maxCoinID, x, y, value);
		loadedCoins.put(newcoin.id, newcoin);
		newCoins.add(newcoin);
		if (autoWrite) {
			DB.coinsDB.insertCoin(newcoin);
		}
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
		updatePlayerLocation(player);
		
		if (autoWrite) {
			DB.coinsDB.deleteCoin(coin);
		}
	}
}
