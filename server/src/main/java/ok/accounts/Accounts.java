package ok.accounts;

import ok.external.*;

import java.sql.SQLException;
import java.util.Optional;

import ok.database.DB;

public class Accounts {
	
	/**
	 * @param token
	 * @return null if DB error
 * 			AccountInfo with only googleid if api succeeds but account does not yet exist
 * 			full AccountInfo if account exists
	 */
	public static AccountInfo getAccountInfo(String googleid) {
		try {
			return DB.accountsDB.query(googleid).orElse(new AccountInfo(googleid));
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return null;
		}
	}
	
	public static boolean createAccount(String token, String handle) {
		String googleid = GoogleAPI.getIDFromIDToken(token);
		if (googleid == null) {
			return false;
		}
		return DB.accountsDB.insert(googleid, handle);
	}
}
