package ok;

import java.util.*;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.*;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	// https://docs.spring.io/spring-framework/docs/4.0.1.RELEASE/spring-framework-reference/html/websocket.html
	
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		System.err.println("registering handler");
		HttpSessionHandshakeInterceptor intercept = new HttpSessionHandshakeInterceptor();
		intercept.setCreateSession(true);
		intercept.setCopyHttpSessionId(true);
		intercept.setCopyAllAttributes(true);
		registry.addHandler(new SocketTextHandler(), "/user")
			.addInterceptors(intercept)
			.setAllowedOrigins("http://localhost");
	}

}
