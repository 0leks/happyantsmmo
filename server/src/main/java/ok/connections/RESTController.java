package ok.connections;

import java.util.*;

import javax.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import ok.accounts.Accounts;
import ok.connections.sessions.*;
import ok.Application;
import ok.accounts.AccountInfo;
import ok.database.DB;
import ok.external.GoogleAPI;
import ok.games.coingame.PlayerInfo;

@RestController
public class RESTController {

	@CrossOrigin(origins = "*")
	@GetMapping("/")
	public ResponseEntity<String> index() {
		return ResponseEntity.status(HttpStatus.OK).body("Greetings from HAMMO Server!");
	}
	
	@CrossOrigin(origins = "*")
	@PostMapping("/googlesignin")
	public ResponseEntity<String> googlesignin(
			@RequestBody String body) {
		String googleid = GoogleAPI.getIDFromIDToken(body);
		if (googleid == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		Session session = SessionManager.getSession(googleid);
		
		// TODO get account info if it exists and return it
		AccountInfo account = Accounts.getAccountInfo(googleid);
		if (account == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("DB error");
		}
		// if not exist, just return different message
		
		return ResponseEntity.status(HttpStatus.OK).body("asdf");
	}
	
	
////	@CrossOrigin(origins = "http://localhost:8080")
	@CrossOrigin(origins = "*")
	@GetMapping("/allaccounts")
	public String allaccountsEndpoint(HttpServletRequest request) {
		System.err.println("allaccountsEndpoint session id: " + request.getSession().getId());
		System.err.println(request.getRemoteHost());
		System.err.println(request.isSecure());
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
	
////	@CrossOrigin(origins = "http://localhost:8080")
	@CrossOrigin(origins = "*")
	@GetMapping("/account")
	public String accountEndpoint(
			@RequestParam("id_token") String id_token
			) {
		System.err.println("GET /ACCOUNT");
//		String token = ctx.queryParam("token");
		System.err.println("id_token = " + id_token);
		AccountInfo info = Accounts.getAccountInfo(id_token);
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
	
//	@CrossOrigin(origins = "*")
//	@PostMapping("/tokensignin")
//	public ResponseEntity<String> tokensignin(
//			@RequestBody String body) {
//		System.err.println(body);
//		
//		GoogleAPI.validateIDToken(body);
//		
//		return ResponseEntity.status(HttpStatus.OK).body("asdf");
//	}
	
////	@CrossOrigin(origins = "http://localhost:8080")
	@CrossOrigin(origins = "*")
	@PostMapping("/account")
	public ResponseEntity<String> accountPostEndpoint(
			@RequestParam("token") String token,
			@RequestBody String body
			) {
		System.err.println("try create acc " + body + ", " + token);
		if (Accounts.createAccount(token, body)) {
			System.err.println("Created account " + body);
			return ResponseEntity.status(HttpStatus.CREATED).body("success");
		}
		else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("fail");
		}
	
	}
}
