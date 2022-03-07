package app;

import static j2html.TagCreator.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.json.*;

import accounts.*;
import io.javalin.Javalin;
import io.javalin.http.HttpCode;
import io.javalin.websocket.*;

public class Chat {
	
	private static final int PORT = 7070;

	private static Map<WsContext, String> userUsernameMap = new ConcurrentHashMap<>();
	private static int nextUserNumber = 1; // Assign to username for next connecting user

	
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
//				String username = userUsernameMap.get(ctx);
//				userUsernameMap.remove(ctx);
				broadcastMessage("Server", "left the chat");
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
			System.err.println("body: " + ctx.body());
			System.err.println("url: " + ctx.fullUrl());
			System.err.println("ctx.queryString(): " + ctx.queryString());
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
			googleIDToContextMap.put(info.googleid, ctx);
			contextToAccountInfoMap.put(ctx, info);
			sendLocation(info);
			break;
		}
			
		case MOVE:
		{
			int x = obj.getInt("x");
			int y = obj.getInt("y");
			AccountInfo info = contextToAccountInfoMap.get(ctx);
			info.x = x;
			info.y = y;
			
			Accounts.updateLocation(info);
			sendLocation(info);
			
			break;
		}
			
		default:
			System.err.println("UNKNOWN MESSAGE TYPE RECEIVED");
		}
	}
	
	private static void sendLocation(AccountInfo info) {
		WsContext ctx = googleIDToContextMap.get(info.googleid);
		if (ctx.session.isOpen()) {
			JSONObject jo = new JSONObject()
					.put("type", MessageType.HELLO)
					.put("x", info.x)
					.put("y", info.y);
			ctx.send(jo.toString());
		}
	}

	// Sends a message from one user to all users, along with a list of current
	// usernames
	private static void broadcastMessage(String sender, String message) {
		userUsernameMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> {
			session.send(new JSONObject().put("userMessage", createHtmlMessageFromSender(sender, message))
					.put("userlist", userUsernameMap.values()).toString());
		});
	}

	// Builds a HTML element with a sender-name, a message, and a timestamp
	private static String createHtmlMessageFromSender(String sender, String message) {
		return article(b(sender + " says:"),
				span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())), p(message)).render();
	}

}
