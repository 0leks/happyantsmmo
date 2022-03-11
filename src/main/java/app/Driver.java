package app;

import java.util.*;
import java.util.concurrent.*;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.json.*;

import accounts.*;
import database.DB;
import games.coingame.*;
import io.javalin.Javalin;
import io.javalin.http.HttpCode;
import io.javalin.websocket.*;

public class Driver {
	
	private static final int PORT = 7070;
	
	public static void main(String[] args) {

		CoinGame coingame = new CoinGame();
		coingame.start();
		
		Javalin app = Javalin.create(config -> {
			config.enableCorsForOrigin("http://localhost");
		}).start(PORT);

		app.ws("/coin", ws -> {
			ws.onClose(ctx -> coingame.closedConnection(ctx));
			ws.onMessage(ctx -> coingame.receiveMessage(ctx));
		});
		
		app.get("/allaccounts", ctx -> {
			List<AccountInfo> accounts = DB.accountsDB.printAllAccounts();
			
			JSONObject jo = new JSONObject();
			jo.put("total-accounts", accounts.size());
			jo.put("currently-playing", coingame.getNumConnections());
			JSONArray accountArray = new JSONArray();
			accounts.forEach(account -> {
				System.err.println(account.handle);
				JSONObject obj = new JSONObject();
				obj.put("handle", account.handle);
				PlayerInfo info = DB.coinsDB.getPlayerInfo(account.id);
				if (info != null) {
					obj.put("numcoins", info.numcoins);
				}
				accountArray.put(obj);
			});
			System.err.println(accountArray.toString());
			jo.put("account-list", accountArray);
			ctx.result(jo.toString());
		});
		app.get("/account", ctx -> {
			System.err.println("GET /ACCOUNT");
			String token = ctx.queryParam("token");
			System.err.println("token = " + token);
			AccountInfo info = Accounts.getAccountInfo(token);
			System.err.println("acc info = " + info);
			if (info == null) {
				ctx.status(HttpCode.IM_A_TEAPOT);
			}
			else {
				System.err.println(info.googleid);
				if (info.handle == null) {
					ctx.result("");
				}
				else {
					ctx.result(info.handle);
				}
			}
		});
		app.post("/account", ctx -> {
			System.err.println("POST /ACCOUNT");
			String token = ctx.queryParam("token");
			System.err.println("try creat acc " + ctx.body() + ", " + token);
			if (Accounts.createAccount(token, ctx.body())) {
				System.err.println("Created account " + ctx.body());
				ctx.result("success");
				ctx.status(HttpCode.CREATED);
			}
			else {
				ctx.result("fail");
				ctx.status(HttpCode.BAD_REQUEST);
			}
		});
	}
}
