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
		
		Session session = SessionManager.getSessionByGoogleID(googleid);
		AccountInfo account = Accounts.getAccountInfo(googleid);
		if (account == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("DB error");
		}
		JSONObject result = new JSONObject();
		result.put("session", session.sessionToken);
		if (account.exists()) {
			session.accountID = account.id;
			// if account exists, return the handle
			// TODO in future maybe return other info as well
			result.put("account", account.toJSONObject());
		}
		return ResponseEntity.status(HttpStatus.OK).body(result.toString());
	}
	
	@CrossOrigin(origins = "*")
	@GetMapping("/allaccounts")
	public String allaccountsEndpoint(HttpServletRequest request) {
		List<AccountInfo> accounts = DB.accountsDB.printAllAccounts();
		
		JSONObject jo = new JSONObject();
		jo.put("total-accounts", accounts.size());
		if (Application.coingame != null) {
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
		}
		return jo.toString();
	}
	
	@CrossOrigin(origins = "*")
	@GetMapping("/account")
	public ResponseEntity<String> accountEndpoint(
			@RequestParam("sessionToken") String sessionToken
			) {
		System.out.println("GET /ACCOUNT sessionToken = " + sessionToken);
		Session session = SessionManager.getSessionBySessionToken(sessionToken);
		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		System.out.println("account id: " + session.accountID);
		AccountInfo info = Accounts.getAccountInfo(session.accountID);
		if (info == null) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		System.out.println("acc info = " + info);
		return ResponseEntity.status(HttpStatus.OK).body(info.toJSONObject().toString());
	}

	@CrossOrigin(origins = "*")
	@GetMapping("/deleteAccount")
	public ResponseEntity<String> deleteAccount(
			@RequestParam("sessionToken") String sessionToken
			) {
		System.out.println("POST /deleteAccount " + sessionToken);
		Session session = SessionManager.getSessionBySessionToken(sessionToken);
		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		if (session.accountID == -1) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		Accounts.deleteAccount(session.accountID);
		SessionManager.terminateSession(session);
		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@CrossOrigin(origins = "*")
	@PostMapping("/account")
	public ResponseEntity<String> accountPostEndpoint(
			@RequestParam("sessionToken") String sessionToken,
			@RequestBody String body
			) {
		System.out.println("POST /ACCOUNT " + body + ", " + sessionToken);
		Session session = SessionManager.getSessionBySessionToken(sessionToken);
		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		if (session.accountID != -1) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		if (Accounts.createAccount(session.googleID, body)) {
			System.err.println("Created account " + body);
			
			AccountInfo info = Accounts.getAccountInfo(session.googleID);
			if (info == null) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("DB error");
			}
			else if (info.handle == null) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to query created account");
			}
			else {
				session.accountID = info.id;
				return ResponseEntity.status(HttpStatus.CREATED).body(info.toJSONObject().toString());
			}
		}
		else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create account.");
		}
	
	}
}
