package ok.connections;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import ok.accounts.Accounts;
import ok.Application;
import ok.accounts.AccountInfo;
import ok.database.DB;
import ok.games.coingame.PlayerInfo;

@RestController
public class HelloController {

//	@CrossOrigin(origins = "http://localhost:8080")
	@CrossOrigin(origins = "*")
	@GetMapping("/")
	public String index() {
		return "Greetings from Spring Boot basic test!";
	}

	@GetMapping("/test")
	public String test() {
		return "test endpoint!";
	}
	
////	@CrossOrigin(origins = "http://localhost:8080")
//	@CrossOrigin(origins = "*")
//	@GetMapping("/allaccounts")
//	public String allaccountsEndpoint() {
//		List<AccountInfo> accounts = DB.accountsDB.printAllAccounts();
//		
//		JSONObject jo = new JSONObject();
//		jo.put("total-accounts", accounts.size());
//		jo.put("currently-playing", Application.coingame.getNumConnections());
//		JSONArray accountArray = new JSONArray();
//		accounts.forEach(account -> {
//			System.err.println(account.handle);
//			JSONObject obj = new JSONObject();
//			obj.put("handle", account.handle);
//			PlayerInfo info = DB.coinsDB.getPlayerInfo(account.id);
//			if (info != null) {
//				obj.put("numcoins", info.numcoins);
//			}
//			accountArray.put(obj);
//		});
//		System.err.println(accountArray.toString());
//		jo.put("account-list", accountArray);
//		return jo.toString();
//	}
//	
////	@CrossOrigin(origins = "http://localhost:8080")
//	@CrossOrigin(origins = "*")
//	@GetMapping("/account")
//	public String accountEndpoint(
//			@RequestParam("token") String token
//			) {
//		System.err.println("GET /ACCOUNT");
////		String token = ctx.queryParam("token");
//		System.err.println("token = " + token);
//		AccountInfo info = Accounts.getAccountInfo(token);
//		System.err.println("acc info = " + info);
//		if (info == null) {
//			return "IM_A_TEAPOT";
//		}
//		else {
//			System.err.println(info.googleid);
//			if (info.handle == null) {
//				return "";
//			}
//			else {
//				return info.handle;
//			}
//		}
//	}
//	
////	@CrossOrigin(origins = "http://localhost:8080")
//	@CrossOrigin(origins = "*")
//	@PostMapping("/account")
//	public ResponseEntity<String> accountPostEndpoint(
//			@RequestParam("token") String token,
//			@RequestBody String body
//			) {
//		System.err.println("try create acc " + body + ", " + token);
//		if (Accounts.createAccount(token, body)) {
//			System.err.println("Created account " + body);
//			return ResponseEntity.status(HttpStatus.CREATED).body("success");
//		}
//		else {
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("fail");
//		}
//	
//	}
}
