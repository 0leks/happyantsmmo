package ok.external;

import java.io.*;
import java.net.*;
import java.util.Optional;

public class GoogleAPI {

	public static Optional<String> getIdFromToken(String token) {
		StringBuilder result = new StringBuilder();
		String urlstring = "https://www.googleapis.com/oauth2/v2/userinfo";
		try {
			URL url = new URL(urlstring);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Authorization", "Bearer " + token);
			conn.setRequestMethod("GET");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				for (String line; (line = reader.readLine()) != null;) {
					result.append(line);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		System.err.println(result);
		String resultString = result.toString();
		if (resultString.contains("\"id\":")) {
			String id = resultString.split("\"id\":")[1].split("\"")[1];
			return Optional.of(id);
		}
		return Optional.empty();
	}
}
