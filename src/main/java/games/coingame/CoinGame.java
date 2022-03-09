package games.coingame;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import org.json.*;

import accounts.*;
import app.MessageType;
import database.DB;
import games.math.Vec2;
import io.javalin.websocket.*;

public class CoinGame {

	private static Map<Integer, WsContext> idToContextMap = new ConcurrentHashMap<>();
	private static Map<WsContext, AccountInfo> contextToAccountInfoMap = new ConcurrentHashMap<>();
	private static Map<WsContext, PlayerInfo> contextToPlayerInfoMap = new ConcurrentHashMap<>();
	
	private static Map<PlayerInfo, Vec2> playerTargetLocations = new ConcurrentHashMap<>();
	
	
	private Thread updateThread;
	private Thread gameThread;
	public CoinGame() {
		updateThread = new Thread(() -> updateFunction());
		gameThread = new Thread(() -> gameFunction());
	}
	
	public int getNumConnections() {
		return idToContextMap.size();
	}
	
	public void start() {
		updateThread.start();
		gameThread.start();
	}
	
	private void newConnection(WsMessageContext ctx, String token) {
		AccountInfo info = Accounts.getAccountInfo(token);
		if (info == null) {
			ctx.session.close();
			return;
		}
		
		// if new connection with an id that matches existing connection, 
		// block it and send message indicating that account is already logged in.
		if (idToContextMap.containsKey(info.id)) {
			ctx.session.close();
		}
		
		// get PlayerInfo
		PlayerInfo playerInfo = DB.coinsDB.getPlayerInfo(info.id);
		if (playerInfo == null) {
			// create new player info
			playerInfo = DB.coinsDB.insertPlayerInfo(info.id);
			if (playerInfo == null) {
				// failed to create new player info.
				ctx.session.close();
			}
		}
		
		idToContextMap.put(info.id, ctx);
		contextToAccountInfoMap.put(ctx, info);
		contextToPlayerInfoMap.put(ctx, playerInfo);
		changedLocations.add(info.id);
		sendAll = true;
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
		playerTargetLocations.put(contextToPlayerInfoMap.get(ctx), new Vec2(x, y));
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
			
		default:
			System.err.println("UNKNOWN MESSAGE TYPE RECEIVED");
		}
	}
	
	private static void updateFunction() {
		try {
			while(true) {
				sendLocations();
				Thread.sleep(200);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static Set<Coin> coins = new HashSet<>();
	private static ConcurrentLinkedQueue<Coin> coinstosend = new ConcurrentLinkedQueue<>();
	
	private static final int PLAYER_SPEED = 50;
	private static void gameFunction() {
		try {
			long timeToNextCoin = System.currentTimeMillis();
			while(true) {
				for (Entry<PlayerInfo, Vec2> entry : playerTargetLocations.entrySet()) {
					PlayerInfo player = entry.getKey();
					Vec2 target = entry.getValue();
					
					Vec2 delta = new Vec2(target.x - player.x, target.y - player.y);
					double distanceLeft = delta.magnitude();
					if (distanceLeft < PLAYER_SPEED) {
						player.x = target.x;
						player.y = target.y;
						playerTargetLocations.remove(player);
					}
					else {
						delta.scale(PLAYER_SPEED/distanceLeft);
						player.x += delta.x;
						player.y += delta.y;
					}
					changedLocations.add(player.id);
					DB.coinsDB.updateLocation(player);
				}
				
				if (System.currentTimeMillis() - timeToNextCoin >= 0) {
					Coin newcoin = Coin.makeCoin((int)(Math.random()*2000) - 1000, (int)(Math.random()*2000) - 1000);
					coinstosend.add(newcoin);
					coins.add(newcoin);
					timeToNextCoin = System.currentTimeMillis() + 5000;
				}
				System.err.println(System.currentTimeMillis()%10000);
				Thread.sleep(400);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	private static Set<Integer> changedLocations = new HashSet<>();
	private static boolean sendAll;
	private static void sendLocations() {
		// TODO add an output queue intermediate here?
		JSONObject jo = new JSONObject();
		jo.put("type", MessageType.MOVE);
		
		JSONArray players = new JSONArray();
		for (PlayerInfo info : contextToPlayerInfoMap.values()) {
			if (changedLocations.contains(info.id) || sendAll) {
				changedLocations.remove(info.id);
				players.put(new JSONObject(info));
			}
		}
		if (!players.isEmpty())
			jo.put("players", players);

		JSONArray coinsArray = new JSONArray();
		while (!coinstosend.isEmpty()) {
			coinsArray.put(new JSONObject(coinstosend.remove()));
		}
		if (!coinsArray.isEmpty()) {
			jo.put("coins", coinsArray);
			System.err.println("sending " + coinsArray.length() + " coins");
		}
		
		sendAll = false;
		String tosend = jo.toString();
		for (WsContext ctx : contextToPlayerInfoMap.keySet()) {
			if (ctx.session.isOpen()) {
				ctx.send(tosend);
			}
		}
	}
}
