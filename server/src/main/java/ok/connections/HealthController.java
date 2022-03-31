package ok.connections;

import org.springframework.web.bind.annotation.*;

import ok.Application;
import ok.database.*;

@RestController
public class HealthController {

	private static String makeH1(String text) {
		return "<h1>" + text + "</h1>";
	}
	private static String makeH2(String text) {
		return "<h2>" + text + "</h2>";
	}
	private static String style = 
			"<style>"
			+ "body {background-color: black; color: lime}"
			+ "</style>";
	
	
	@GetMapping("/health")
	public String health() {
		return "<html>\n<header><title>Health</title>" + style + "</header>\n<body>\n"
				+ makeH1("HAMMO Health Page")
				+ makeH2("Database String: " + DB.getConnectionString())
				+ makeH2("DB Connected: " + DB.isConnected())
				+ makeH2("Tables: " + String.join(", ", DBUtil.getTables()))
				+ makeH2("debug: " + DB.debug)
				+ makeH2("coingame: " + Application.coingame)
		        + "</body>\n</html>";
	}
}
