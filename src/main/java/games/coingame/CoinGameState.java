package games.coingame;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import database.DB;
import games.math.Vec2;

public class CoinGameState {

	private Map<Integer, Coin> loadedCoins = new ConcurrentHashMap<>();
	private Map<Integer, PlayerInfo> loadedPlayerInfo = new ConcurrentHashMap<>();
	private Map<Integer, Coin> collectedCoins = new ConcurrentHashMap<>();
	
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
		for (Coin coin : loadedCoins.values()) {
			DB.coinsDB.insertCoin(coin);
		}
		System.out.println("Saved " + collectedCoins.size() + " collected coins to DB");
		for(Integer coinid : collectedCoins.keySet()) {
			DB.coinsDB.deleteCoin(collectedCoins.remove(coinid));
		}
		System.out.println("Saved " + loadedPlayerInfo.size() + " players to DB");
		System.out.println("Saved " + loadedCoins.size() + " coins to DB");
	}
	
	public PlayerInfo getPlayerInfo(int id) {
		if (!loadedPlayerInfo.containsKey(id)) {
			loadedPlayerInfo.put(id, DB.coinsDB.getPlayerInfo(id));
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
	
	public void addNewCoin(int x, int y) {
		Coin newcoin = new Coin(++maxCoinID, x, y);
		loadedCoins.put(newcoin.id, newcoin);
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
		collectedCoins.put(coin.id, coin);
		player.numcoins++;
		updatePlayerLocation(player);
		
		if (autoWrite) {
			DB.coinsDB.collected(player.id, coin.id);
		}
	}
}
