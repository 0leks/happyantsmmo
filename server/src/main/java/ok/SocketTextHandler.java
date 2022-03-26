package ok;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

import org.json.JSONObject;
import org.springframework.stereotype.*;

@Component
public class SocketTextHandler extends TextWebSocketHandler {
	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws InterruptedException, IOException {
		
		System.err.println("received message");

		String payload = message.getPayload();
//		JSONObject jsonObject = new JSONObject(payload);
		session.sendMessage(new TextMessage("Hi " + payload + " how may we help you? asdf"));
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		System.err.println("established connection");
		session.sendMessage(new TextMessage("hello asdf"));
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		System.err.println("afterConnectionClosed asdf");
	}
	
	
}
