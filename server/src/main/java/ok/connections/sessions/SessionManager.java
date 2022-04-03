package ok.connections.sessions;

import java.util.Map;

public class SessionManager {
	
//	private Map<String, String> 
	private static Map<String, Session> sessionTokenToSession;
	private static Map<String, Session> googleidToSession;
	
	public static Session getSession(String googleid) {
		if (!googleidToSession.containsKey(googleid)) {
			Session newSession = Session.createSession(googleid);
			googleidToSession.put(googleid, newSession);
		}
		return googleidToSession.get(googleid);
	}
}
