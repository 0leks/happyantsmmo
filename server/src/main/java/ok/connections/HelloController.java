package ok.connections;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ok.accounts.Accounts;
import ok.Application;
import ok.accounts.AccountInfo;
import ok.database.DB;
import ok.games.coingame.PlayerInfo;

@RestController
public class HelloController {

	@GetMapping("/")
	public String index() {
		return "Greetings from Spring Boot!";
	}
	
	@GetMapping("/allaccounts")
	public String allaccountsEndpoint() {
		List<AccountInfo> accounts = DB.accountsDB.printAllAccounts();
		
		JSONObject jo = new JSONObject();
		jo.put("total-accounts", accounts.size());
		jo.put("currently-playing", Application.coingame.getNumConnections());
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
		return jo.toString();
	}
	
	@GetMapping("/account")
	public String accountEndpoint(
			@RequestParam("token") String token
			) {
		System.err.println("GET /ACCOUNT");
//		String token = ctx.queryParam("token");
		System.err.println("token = " + token);
		AccountInfo info = Accounts.getAccountInfo(token);
		System.err.println("acc info = " + info);
		if (info == null) {
			return "IM_A_TEAPOT";
		}
		else {
			System.err.println(info.googleid);
			if (info.handle == null) {
				return "";
			}
			else {
				return info.handle;
			}
		}
	}
	@PostMapping("/account")
	public void accountPostEndpoint() {
		
		// TODO
		
//		System.err.println("POST /ACCOUNT");
//		String token = ctx.queryParam("token");
//		System.err.println("try creat acc " + ctx.body() + ", " + token);
//		if (Accounts.createAccount(token, ctx.body())) {
//			System.err.println("Created account " + ctx.body());
//			ctx.result("success");
//			ctx.status(HttpCode.CREATED);
//		}
//		else {
//			ctx.result("fail");
//			ctx.status(HttpCode.BAD_REQUEST);
//		}
	
	}
}
