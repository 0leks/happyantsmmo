package ok.accounts;

import java.util.*;

import ok.external.*;
import ok.database.DB;

public class Accounts {
	
	/**
	 * @param token
	 * @return null if google api call fails
 * 			AccountInfo with only googleid if api succeeds but account does not yet exist
 * 			full AccountInfo if account exists
	 */
	public static AccountInfo getAccountInfo(String token) {
		String googleid = GoogleAPI.getIDFromIDToken(token);
		if (googleid == null) {
			return null;
		}
		return DB.accountsDB.query(googleid).orElse(new AccountInfo(googleid));
	}
	
	public static boolean createAccount(String token, String handle) {
		String googleid = GoogleAPI.getIDFromIDToken(token);
		if (googleid == null) {
			return false;
		}
		return DB.accountsDB.insert(googleid, handle);
	}
}
