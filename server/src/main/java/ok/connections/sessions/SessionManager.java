package ok.connections.sessions;

import java.util.*;

public class SessionManager {
	
//	private Map<String, String> 
	private static Map<String, Session> sessionTokenToSession = new HashMap<>();
	private static Map<String, Session> googleidToSession = new HashMap<>();
	
	public static void terminateSession(Session session) {
		sessionTokenToSession.remove(session.sessionToken);
		googleidToSession.remove(session.googleID);
	}

	public static Session getSessionByGoogleID(String googleID) {
		if (!googleidToSession.containsKey(googleID)) {
			Session newSession = Session.createSession(googleID);
			googleidToSession.put(googleID, newSession);
			sessionTokenToSession.put(newSession.sessionToken, newSession);
		}
		return googleidToSession.get(googleID);
	}
	
	public static Session getSessionBySessionToken(String sessionToken) {
		return sessionTokenToSession.get(sessionToken);
	}
}
