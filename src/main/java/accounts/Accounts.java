package accounts;

import java.util.*;

import app.GoogleAPI;
import database.DB;

public class Accounts {
	
	/**
	 * @param token
	 * @return null if google api call fails
 * 			AccountInfo with only googleid if api succeeds but account does not yet exist
 * 			full AccountInfo if account exists
	 */
	public static AccountInfo getAccountInfo(String token) {
		Optional<String> googleidOpt = GoogleAPI.getIdFromToken(token);
		if (googleidOpt.isEmpty()) {
			return null;
		}
		return DB.accountsDB.query(googleidOpt.get()).orElse(new AccountInfo(googleidOpt.get()));
	}
	
	public static boolean createAccount(String token, String handle) {
		Optional<String> googleidOpt = GoogleAPI.getIdFromToken(token);
		if (googleidOpt.isEmpty()) {
			return false;
		}
		return DB.accountsDB.insert(googleidOpt.get(), handle);
	}
}
