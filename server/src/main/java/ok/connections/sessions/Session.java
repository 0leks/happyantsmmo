package ok.connections.sessions;

import java.util.Random;

public class Session {
	
	private static final long DAY = 1000*60*60*24;
	private static final Random generator = new Random();
	
	public final String sessionToken;
	public final String googleID;
	public long expires;
	public int accountID;
	
	private Session(String sessionToken, String googleID, int accountID) {
		this.sessionToken = sessionToken;
		this.googleID = googleID;
		this.accountID = accountID;
		this.expires = System.currentTimeMillis() + DAY;
	}
	
	public boolean isValidAccount() {
		return accountID != -1;
	}

	public static Session createSession(String googleID) {
		Random rand = new Random(googleID.hashCode() + generator.nextLong());
		String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 32; i++) {
			sb.append(validChars.charAt(rand.nextInt(validChars.length())));
		}
		return new Session(sb.toString(), googleID, -1);
	}
}
