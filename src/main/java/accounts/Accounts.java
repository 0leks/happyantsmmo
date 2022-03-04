package accounts;

import java.io.*;

import app.GoogleAPI;

public class Accounts {
	
	private static final String ACCOUNTS_DIR = "./data/accounts/";
	
	
	
	public static AccountInfo getAccountInfo(String token) {
		
		String id = GoogleAPI.getIdFromToken(token);
		if (id == null) {
			return null;
		}
		
		try (BufferedReader reader = new BufferedReader(
				new FileReader(ACCOUNTS_DIR + id))) {
			String handle = reader.readLine();
			return new AccountInfo(id, handle);
		} catch (FileNotFoundException e) {
			// account does not exist 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new AccountInfo(id, null);
	}
	
	public static boolean createAccount(String token, String handle) {
		String id = GoogleAPI.getIdFromToken(token);
		if (id == null) {
			return false;
		}
		try (PrintWriter writer = new PrintWriter(
				new FileWriter(ACCOUNTS_DIR + id))) {
			writer.println(handle);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
