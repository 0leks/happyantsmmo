package app;

import java.util.*;
import java.util.concurrent.*;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.json.*;

import accounts.*;
import game.Coin;
import io.javalin.Javalin;
import io.javalin.http.HttpCode;
import io.javalin.websocket.*;

public class Chat {
	
	private static final int PORT = 7070;
	
	private static Map<String, WsContext> googleIDToContextMap = new ConcurrentHashMap<>();
	private static Map<WsContext, AccountInfo> contextToAccountInfoMap = new ConcurrentHashMap<>();
	public static void main(String[] args) {
		
		Accounts.connectToDB();
		
		Javalin app = Javalin.create(config -> {
			config.enableCorsForOrigin("http://localhost");
		}).start(PORT);

		app.ws("/play", ws -> {
			ws.onConnect(ctx -> {
//				String username = "User" + nextUserNumber++;
//				userUsernameMap.put(ctx, username);
//				broadcastMessage("Server", (username + " joined the chat"));
			});
			ws.onClose(ctx -> {
				AccountInfo info = contextToAccountInfoMap.get(ctx);
				System.err.println(info.handle + " disconnected");
				googleIDToContextMap.remove(info.googleid);
				contextToAccountInfoMap.remove(ctx);
			});
			ws.onMessage(ctx -> receiveMessage(ctx));
		});
		
		
		app.get("/allaccounts", ctx -> {
			List<AccountInfo> accounts = Accounts.printAllAccounts();
			
			JSONObject jo = new JSONObject();
			jo.put("total-accounts", accounts.size());
			jo.put("currently-playing", 0);
			JSONArray accountArray = new JSONArray();
			accounts.forEach(account -> accountArray.put(account.handle));
			jo.put("account-list", accountArray);
			ctx.result(jo.toString());
		});
		app.get("/account", ctx -> {
//			System.err.println("body: " + ctx.body());
//			System.err.println("url: " + ctx.fullUrl());
//			System.err.println("ctx.queryString(): " + ctx.queryString());
			String token = ctx.queryParam("token");
			System.err.println(token);
			AccountInfo info = Accounts.getAccountInfo(token);
			if (info.handle == null) {
				ctx.result("");
			}
			else {
				ctx.result(info.handle);
			}
		});
		app.post("/account", ctx -> {
//			System.err.println("body: " + ctx.body());
//			System.err.println("url: " + ctx.fullUrl());
//			System.err.println("ctx.queryString(): " + ctx.queryString());
			String token = ctx.queryParam("token");
			if (Accounts.createAccount(token, ctx.body())) {
				ctx.result("success");
				ctx.status(HttpCode.CREATED);
			}
			else {
				ctx.result("fail");
				ctx.status(HttpCode.BAD_REQUEST);
			}
		});
		
		Thread updateThread = new Thread(() -> updateFunction());
		Thread gameThread = new Thread(() -> gameFunction());
		updateThread.start();
		gameThread.start();
	}
	
	public static void receiveMessage(WsMessageContext ctx) {
		String message = ctx.message();
		JSONObject obj = new JSONObject(message);
		
		MessageType type = MessageType.valueOf(obj.getString("type"));
		
		switch(type) {
		case HELLO:
		{
			String token = obj.getString("token");
			System.err.println(token);
			AccountInfo info = Accounts.getAccountInfo(token);
			System.err.println(info.handle + " joined game");
			if (googleIDToContextMap.containsKey(info.googleid)) {
				WsContext oldContext = googleIDToContextMap.get(info.googleid);
				contextToAccountInfoMap.remove(oldContext);
				oldContext.session.close();
			}
			googleIDToContextMap.put(info.googleid, ctx);
			contextToAccountInfoMap.put(ctx, info);
			changedLocations.add(info.id);
			sendAll = true;
			break;
		}
			
		case MOVE:
		{
			int x = obj.getInt("x");
			int y = obj.getInt("y");
			AccountInfo info = contextToAccountInfoMap.get(ctx);
			info.x = x;
			info.y = y;
			changedLocations.add(info.id);
			
			Accounts.updateLocation(info);
			sendLocations();
			
			break;
		}
			
		default:
			System.err.println("UNKNOWN MESSAGE TYPE RECEIVED");
		}
	}
	
	private static void updateFunction() {
		try {
			while(true) {
				sendLocations();
				Thread.sleep(800);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static Set<Coin> coins = new HashSet<>();
	private static ConcurrentLinkedQueue<Coin> coinstosend = new ConcurrentLinkedQueue<>();
	
	private static void gameFunction() {
		try {
			while(true) {
				Coin newcoin = Coin.makeCoin((int)(Math.random()*500), (int)(Math.random()*500));
				coinstosend.add(newcoin);
				coins.add(newcoin);
				Thread.sleep(5000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	private static Set<Integer> changedLocations = new HashSet<>();
	private static boolean sendAll;
	private static void sendLocations() {
		JSONObject jo = new JSONObject()
				.put("type", MessageType.MOVE);
		
		JSONArray players = new JSONArray();
		for (AccountInfo info : contextToAccountInfoMap.values()) {
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
		if (!coinsArray.isEmpty())
			jo.put("coins", coinsArray);
		
		sendAll = false;
		String tosend = jo.toString();
		for (WsContext ctx : contextToAccountInfoMap.keySet()) {
			if (ctx.session.isOpen()) {
				ctx.send(tosend);
			}
		}
	}
}
