package ok.external;

import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;
import java.util.*;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.json.JsonFactory;

import com.google.api.client.json.gson.GsonFactory;

public class GoogleAPI {
	
	private static String CLIENT_ID = "729886368028-j3f6iq0nshp3vog9bet3cu79ms34r00s.apps.googleusercontent.com";
	
	private static HttpTransport httptransport;
	private static JsonFactory jsonFactory = new GsonFactory();
	private static GoogleIdTokenVerifier verifier;
	
	static {
		try {
			httptransport = GoogleNetHttpTransport.newTrustedTransport();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		verifier = new GoogleIdTokenVerifier.Builder(httptransport, jsonFactory)
			    // Specify the CLIENT_ID of the app that accesses the backend:
			    .setAudience(Collections.singletonList(CLIENT_ID))
			    .build();
	}

//	public static Optional<String> getIdFromToken(String token) {
//		StringBuilder result = new StringBuilder();
//		String urlstring = "https://www.googleapis.com/oauth2/v2/userinfo";
//		try {
//			URL url = new URL(urlstring);
//			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//			conn.setRequestProperty("Authorization", "Bearer " + token);
//			conn.setRequestMethod("GET");
//			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
//				for (String line; (line = reader.readLine()) != null;) {
//					result.append(line);
//				}
//			}
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
////		System.err.println(result);
//		String resultString = result.toString();
//		if (resultString.contains("\"id\":")) {
//			String id = resultString.split("\"id\":")[1].split("\"")[1];
//			return Optional.of(id);
//		}
//		return Optional.empty();
//	}
	
	
	public static String getIDFromIDToken(String idTokenString) {
		GoogleIdToken idToken = null;
		try {
			idToken = verifier.verify(idTokenString);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (idToken == null) {
			System.out.println("Invalid ID token.");
			return null;
		}
		Payload payload = idToken.getPayload();

		// Print user identifier
		String userId = payload.getSubject();
		System.out.println("User ID: " + userId);

//			// Get profile information from payload
//			String email = payload.getEmail();
//			boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
//			String name = (String) payload.get("name");
//			String pictureUrl = (String) payload.get("picture");
//			String locale = (String) payload.get("locale");
//			String familyName = (String) payload.get("family_name");
//			String givenName = (String) payload.get("given_name");
//
//			System.out.println(email);
//			System.out.println(emailVerified);
//			System.out.println(name);
//			System.out.println(pictureUrl);
//			System.out.println(locale);
//			System.out.println(familyName);
//			System.out.println(givenName);

		return userId;
	}

}
