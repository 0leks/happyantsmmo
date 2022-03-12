package games.coingame;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import org.json.*;

import accounts.*;
import app.MessageType;
import games.math.*;
import io.javalin.websocket.*;

import static app.Util.currentTime;

public class CoinGame {
	
	private static final int TICK_TIME = 333;
	private static final int UPDATE_TIME = 333;
	private static final int PLAYER_SPEED = 1000;
	
	/**
	 * This is the main map that gets iterated to send updates to all contexts
	 */
	private Map<WsContext, PlayerInfo> contextToPlayerInfoMap = new ConcurrentHashMap<>();

	private Map<Integer, WsContext> idToContextMap = new HashMap<>();
	private Map<WsContext, AccountInfo> contextToAccountInfoMap = new HashMap<>();
	private Map<PlayerInfo, Vec3> playerTargetLocations = new HashMap<>();

	/**
	 * if null, need to share all coins
	 * if has a hashmap, then cointains list of coin ids that have been shared
	 */
	private Map<WsContext, Set<Integer>> contextToCoinsShared = new HashMap<>();	
	
	private ConcurrentLinkedQueue<Coin> deletedCoins = new ConcurrentLinkedQueue<>();

	
	private Set<Integer> changedLocations = new HashSet<>();
	private boolean sendAllPlayerLocations;
	
	private CoinGameState state;
	
	private Thread updateThread;
	private Thread gameThread;
	private Thread databaseSaver;
	private volatile boolean stopGame;
	
	public CoinGame() {
		state = new CoinGameState(false);
		updateThread = new Thread(() -> updateFunction());
		gameThread = new Thread(() -> gameFunction());
		databaseSaver = new Thread(() -> databaseSaveFunction());
	}
	
	public int getNumConnections() {
		return idToContextMap.size();
	}
	
	public void start() {
		updateThread.start();
		gameThread.start();
		databaseSaver.start();
	}
	
	private void stopGame(WsMessageContext ctx) {
		// only admin account can stop the game
		System.out.println(contextToPlayerInfoMap.get(ctx) + " tried to stop game");
		if (contextToPlayerInfoMap.get(ctx).id != 1) {
			return;
		}
		
		JSONObject message = new JSONObject();
		message.put("type", MessageType.STOP);
		message.put("message", "Server is closing for maintenance in 5 seconds");
		String tosend = message.toString();
		for (WsContext playerContext : contextToPlayerInfoMap.keySet()) {
			playerContext.send(tosend);
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		for (WsContext playerContext : contextToPlayerInfoMap.keySet()) {
			playerContext.session.close();
		}
		
		stopGame = true;
		try {
			updateThread.join(UPDATE_TIME*4);
			gameThread.join(UPDATE_TIME*4);
			databaseSaver.join(UPDATE_TIME*4);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		state.persist();
	}
	
	private void newConnection(WsMessageContext ctx, String token) {
		AccountInfo info = Accounts.getAccountInfo(token);
		if (info == null || info.handle == null || stopGame) {
			ctx.session.close();
			return;
		}
		
		// if new connection with an id that matches existing connection, 
		// block it and send message indicating that account is already logged in.
		if (idToContextMap.containsKey(info.id)) {
			ctx.session.close();
		}
		
		PlayerInfo playerInfo = state.getPlayerInfo(info.id);
		if (playerInfo == null) {
			playerInfo = state.createNewPlayer(info.id);
			if (playerInfo == null) {
				// failed to create new player info.
				ctx.session.close();
			}
		}
		
		contextToCoinsShared.put(ctx, new HashSet<>());
		idToContextMap.put(info.id, ctx);
		contextToAccountInfoMap.put(ctx, info);
		contextToPlayerInfoMap.put(ctx, playerInfo);
		changedLocations.add(info.id);
		sendAllPlayerLocations = true;
		System.err.println(info.handle + " joined game");
		
		JSONObject obj = new JSONObject(playerInfo);
		obj.put("type", MessageType.HELLO);
		ctx.send(obj.toString());
	}
	
	
	
	public void closedConnection(WsCloseContext ctx) {
		AccountInfo info = contextToAccountInfoMap.get(ctx);
		idToContextMap.remove(info.id);
		contextToAccountInfoMap.remove(ctx);
		contextToPlayerInfoMap.remove(ctx);
		System.err.println(info.handle + " disconnected");
	}
	
	public void receiveMove(WsMessageContext ctx, int x, int y) {
		movePlayer(contextToPlayerInfoMap.get(ctx));
		playerTargetLocations.put(contextToPlayerInfoMap.get(ctx), new Vec3(x, y, currentTime()));
	}
	
	public void receiveMessage(WsMessageContext ctx) {
		String message = ctx.message();
		JSONObject obj = new JSONObject(message);
		MessageType type = MessageType.valueOf(obj.getString("type"));
		
		switch(type) {
		case HELLO:
			newConnection(ctx, obj.getString("token"));
			break;
			
		case MOVE:
			receiveMove(ctx, obj.getInt("x"), obj.getInt("y"));
			break;

		case STOP:
			stopGame(ctx);
			break;
			
		default:
			System.err.println("UNKNOWN MESSAGE TYPE RECEIVED");
		}
	}
	
	private void movePlayer(PlayerInfo player) {
		if (!playerTargetLocations.containsKey(player)) {
			return;
		}
		Vec3 interpolatedPosition = getInterpolatedPlayerPosition(player);
		player.x = interpolatedPosition.x;
		player.y = interpolatedPosition.y;
		player.x = Math.max(Math.min(player.x, 10000), -10000);
		player.y = Math.max(Math.min(player.y, 10000), -10000);

		if (playerTargetLocations.containsKey(player)) {
			Vec3 fullTarget = playerTargetLocations.get(player);
			if(interpolatedPosition == fullTarget) {
				playerTargetLocations.remove(player);
				System.out.println(player.id + " reached destination");
			}
			else {
				fullTarget.z = currentTime();
			}
		}
		changedLocations.add(player.id);
		state.updatePlayerLocation(player);
	}
	
	private Vec3 getInterpolatedPlayerPosition(PlayerInfo player) {
		if (!playerTargetLocations.containsKey(player)) {
			return new Vec3(player.x, player.y, 0);
		}
		Vec3 fullTarget = playerTargetLocations.get(player);
		Vec2 targetLocation = fullTarget.xy();
		int elapsedTime = currentTime() - fullTarget.z;
		
		Vec2 deltaLocation = new Vec2(targetLocation.x - player.x, targetLocation.y - player.y);
		double distanceLeft = deltaLocation.magnitude();
		
		double distanceTravelled = elapsedTime * PLAYER_SPEED / 1000.0;
		if (distanceTravelled >= distanceLeft) {
			return fullTarget;
		}
		else {
			deltaLocation.scale(distanceTravelled/distanceLeft);
			return new Vec3(player.x + deltaLocation.x, player.y + deltaLocation.y, 0);
		}
	}
	
	private void movePlayers() {
		for (PlayerInfo info : contextToPlayerInfoMap.values()) {
			movePlayer(info);
		}
	}
	
	private void checkCoinCollect() {
		for (PlayerInfo info : contextToPlayerInfoMap.values()) {
			
			List<Coin> coins = state.getCoinsInRange(
					new Vec2(info.x - 300, info.y - 300), 
					new Vec2(info.x + 300, info.y + 300));
//			System.err.println("close to " + coins.size() + " coins");
			for(Coin c : coins) {
				state.playerCollectsCoin(info, c);
				deletedCoins.add(c);
			}
		}
	}
	
	private void gameFunction() {
		try {
			long timeToNextCoin = currentTime();
			while(!stopGame) {
				long starttime = currentTime();
				movePlayers();
				checkCoinCollect();
				
				if (currentTime() - timeToNextCoin >= 0) {
					state.addNewCoin((int)(Math.random()*20000) - 10000, (int)(Math.random()*20000) - 10000);
					timeToNextCoin = currentTime() + TICK_TIME;
					timeToNextCoin += Math.max(0, 20000 - TICK_TIME*contextToPlayerInfoMap.size());
				}
//				System.out.println("game tick: " + currentTime()%10000);
				long deltatime = currentTime() - starttime;
				long timetosleep = TICK_TIME - deltatime;
				if (timetosleep > 0) {
					Thread.sleep(timetosleep);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.err.println("finished gameFunction");
	}

//	private volatile int lastSentUpdate;
	private void updateFunction() {
//		long lastSent = 0;
		try {
			while(!stopGame) {
				long starttime = currentTime();
				sendLocations();
				long endtime = currentTime();

//				long timestamp = currentTime();
//				long timeSinceLastSent = timestamp - lastSent;
//				String status = String.format("%d delta %d", timestamp%10000, timeSinceLastSent);
//				System.out.println(status);
				
				long timetosleep = UPDATE_TIME - (endtime - starttime);
				if(timetosleep >= 50) {
					Thread.sleep(timetosleep);
				}
//				lastSent = timestamp;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.err.println("finished updateFunction");
	}
	private void sendLocations() {
//		lastSentUpdate = currentTime();
		// TODO add an output queue intermediate here?
		JSONObject jo = new JSONObject();
		jo.put("type", MessageType.MOVE);
		
		JSONArray players = new JSONArray();
		for (PlayerInfo info : contextToPlayerInfoMap.values()) {
			if (changedLocations.contains(info.id) || sendAllPlayerLocations) {
				changedLocations.remove(info.id);
				
				Vec3 location = getInterpolatedPlayerPosition(info);
				JSONObject obj = new JSONObject(info);
				obj.put("x", location.x);
				obj.put("y", location.y);
				
				if (playerTargetLocations.containsKey(info)) {
					obj.put("target", new JSONObject(playerTargetLocations.get(info)));
				}
				players.put(obj);
			}
		}
		if (!players.isEmpty())
			jo.put("players", players);

		sendAllPlayerLocations = false;
		for (WsContext ctx : contextToPlayerInfoMap.keySet()) {
			if (ctx.session.isOpen()) {
				
				JSONArray coinsArray = new JSONArray();
				Set<Integer> alreadySharedCoins = contextToCoinsShared.get(ctx);
				for(Coin coin : state.getCoins()) {
					if (!alreadySharedCoins.contains(coin.id)) {
						coinsArray.put(new JSONObject(coin));
						alreadySharedCoins.add(coin.id);
					}
				}
				
				// always share all deleted coins
				while(!deletedCoins.isEmpty()) {
					coinsArray.put(new JSONObject(deletedCoins.remove()).put("delete", true));
				}
				if (!coinsArray.isEmpty()) {
					jo.put("coins", coinsArray);
//					System.err.println("sending " + coinsArray.length() + " coins");
				}

				String tosend = jo.toString();
				ctx.send(tosend);
			}
		}
	}
	
	private void databaseSaveFunction() {
		try {
			while(!stopGame) {
				state.persist();
				for(int i = 0; i < 10000/UPDATE_TIME && !stopGame; i++) {
					Thread.sleep(UPDATE_TIME);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.err.println("finished databaseSaveFunction");
	
	}
}
