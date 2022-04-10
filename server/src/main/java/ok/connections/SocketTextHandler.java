package ok.connections;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import ok.Application;

@Component
public class SocketTextHandler extends TextWebSocketHandler {
	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws InterruptedException, IOException {
		System.out.println("received message " + message);
		Application.coingame.receiveMessage(session, message);
		
//		System.err.println("received message");
//
//		String payload = message.getPayload();
////		JSONObject jsonObject = new JSONObject(payload);
//		session.sendMessage(new TextMessage("Hi " + payload + " how may we help you?"));
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		System.out.println("established connection with " + session.getRemoteAddress());
//		session.sendMessage(new TextMessage("hello"));
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		System.out.println("afterConnectionClosed");
		
		Application.coingame.closedConnection(session);
	}
	
	
}
