package app;

import static j2html.TagCreator.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import accounts.*;
import io.javalin.Javalin;
import io.javalin.http.HttpCode;
import io.javalin.websocket.WsContext;

public class Chat {
	
	private static final int PORT = 7070;

	private static Map<WsContext, String> userUsernameMap = new ConcurrentHashMap<>();
	private static int nextUserNumber = 1; // Assign to username for next connecting user

	public static void main(String[] args) {
		Javalin app = Javalin.create(config -> {
			config.enableCorsForOrigin("http://localhost");
		}).start(PORT);

		app.ws("/chat", ws -> {
			ws.onConnect(ctx -> {
				String username = "User" + nextUserNumber++;
				userUsernameMap.put(ctx, username);
				broadcastMessage("Server", (username + " joined the chat"));
			});
			ws.onClose(ctx -> {
				String username = userUsernameMap.get(ctx);
				userUsernameMap.remove(ctx);
				broadcastMessage("Server", (username + " left the chat"));
			});
			ws.onMessage(ctx -> {
				broadcastMessage(userUsernameMap.get(ctx), ctx.message());
			});
		});
		
		app.get("/account", ctx -> {
			System.err.println("body: " + ctx.body());
			System.err.println("url: " + ctx.fullUrl());
			System.err.println("ctx.queryString(): " + ctx.queryString());
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
