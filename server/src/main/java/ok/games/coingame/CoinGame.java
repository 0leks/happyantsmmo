package ok.games.coingame;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.swing.JOptionPane;

import org.json.*;
import org.springframework.http.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import ok.accounts.*;
import ok.connections.*;
import ok.connections.sessions.*;
import ok.games.coingame.tunnels.*;
import ok.games.math.*;
//import io.javalin.websocket.*;
import ok.util.Util;

import static ok.util.Util.currentTime;

public class CoinGame {
	
	private static final int TICK_TIME = 300;
	private static final int UPDATE_TIME = 2000;
	private static final int PLAYER_SPEED = 1000;
	private static final int TUNNEL_SIZE = 250;
	
	/**
	 * This is the main map that gets iterated to send updates to all contexts
	 */
	private Map<WebSocketSession, PlayerInfo> contextToPlayerInfoMap = new ConcurrentHashMap<>();

	private Map<Integer, WebSocketSession> idToContextMap = new HashMap<>();
	private Map<WebSocketSession, AccountInfo> contextToAccountInfoMap = new HashMap<>();
	private Map<PlayerInfo, PlayerAction> playerTargetLocations = new HashMap<>();

	private ConcurrentLinkedQueue<AccountInfo> newConnectionNotification = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<AccountInfo> endedConnectionNotification = new ConcurrentLinkedQueue<>();
	
	/**
	 * if null, need to share all coins
	 * if has a hashmap, then cointains list of coin ids that have been shared
	 */
	private ConcurrentLinkedQueue<Coin> newCoins = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<Coin> deletedCoins = new ConcurrentLinkedQueue<>();

	
	private Set<Integer> changedLocations = new HashSet<>();
	private boolean sendAllPlayerLocations;
	
	private CoinGameState state;
	
	private Thread updateThread;
	private Thread gameThread;
	private Thread databaseSaver;
	private volatile boolean stopGame;
	
	public CoinGame() {
		state = new CoinGameState(false);
		updateThread = new Thread(() -> updateFunction());
		gameThread = new Thread(() -> gameFunction());
		databaseSaver = new Thread(() -> databaseSaveFunction());
	}
	
	public int getNumConnections() {
		return idToContextMap.size();
	}
	
	public void start() {
		updateThread.start();
		gameThread.start();
		databaseSaver.start();
	}
	
	private void stopGame(PlayerInfo player) {
		System.out.println(player + " tried to stop game");
		
		// only admin account can stop the game
		if (player.id != 1) {
			return;
		}
		
		sendToAll("Server is closing for maintenance in 20 seconds");
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		for (WebSocketSession playerContext : contextToPlayerInfoMap.keySet()) {
			try {
				playerContext.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		stopGame = true;
		try {
			updateThread.join(UPDATE_TIME*4);
			gameThread.join(UPDATE_TIME*4);
			databaseSaver.join(UPDATE_TIME*4);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		state.persist();
	}
	
	private void newConnection(WebSocketSession ctx, String sessionToken) {
		Session session = SessionManager.getSessionBySessionToken(sessionToken);
		if (session == null || !session.isValidAccount()) {
			sendBye(ctx, "Invalid session");
			return;
		}
		if (stopGame) {
			sendBye(ctx, "Server stopped for maintenance");
			return;
		}
		AccountInfo info = Accounts.getAccountInfo(session.accountID);
		if (info == null || info.handle == null) {
			sendBye(ctx, "Couldn't find account info");
			return;
		}
		
		// if new connection with an id that matches existing connection, 
		// block it and send message indicating that account is already logged in.
		if (idToContextMap.containsKey(info.id)) {
			sendBye(ctx, "This account is already connected on another session.");
			return;
		}
		
		PlayerInfo playerInfo = state.getPlayerInfo(info.id);
		if (playerInfo == null) {
			playerInfo = state.createNewPlayer(info.id);
			if (playerInfo == null) {
				sendBye(ctx, "Failed to create new player info");
				return;
			}
		}
		
		idToContextMap.put(info.id, ctx);
		contextToAccountInfoMap.put(ctx, info);
		contextToPlayerInfoMap.put(ctx, playerInfo);
		changedLocations.add(info.id);
		sendAllPlayerLocations = true;
		System.err.println(info.handle + " joined game");
		
		JSONObject obj = new JSONObject(playerInfo);
		System.out.println(obj.toString());
		obj.put("type", MessageType.HELLO);
		sendToOne(obj.toString(), ctx);
		shareAllCoinsWith(ctx);
		newConnectionNotification.add(info);
		shareAllPlayerMappingWith(ctx);
		shareAllTunnelsOf(ctx, playerInfo);
	}
	
	private void shareAllTunnelsOf(WebSocketSession ctx, PlayerInfo info) {
		Set<TunnelNode> tunnelNodes = new HashSet<>();
		List<TunnelSegment> tunnels = state.getTunnelsOfPlayer(info.id, tunnelNodes);
		JSONArray nodeArr = new JSONArray();
		for(TunnelNode node : tunnelNodes) {
			nodeArr.put(new JSONObject(node));
		}
		JSONArray segmentArr = new JSONArray();
		for(TunnelSegment tunnel : tunnels) {
			segmentArr.put(new JSONObject(tunnel));
		}
		JSONObject obj = new JSONObject().put("type", MessageType.TUNNEL);
		if (nodeArr.length() > 0 || segmentArr.length() > 0) {
			obj.put("tunnelnodes", nodeArr);
			obj.put("tunnelsegments", segmentArr);
			sendToOne(obj.toString(), ctx);
		}
	}
	
	private void shareAllCoinsWith(WebSocketSession ctx) {
		JSONObject obj = new JSONObject().put("type", MessageType.COIN);
		JSONArray coinsArray = new JSONArray();
		for(Coin coin : state.getCoins()) {
			coinsArray.put(new JSONObject(coin));
		}
		if (coinsArray.length() > 0) {
			obj.put("coins", coinsArray);
			sendToOne(obj.toString(), ctx);
		}
	}
	
	public void closedConnection(WebSocketSession ctx) {
		AccountInfo info = contextToAccountInfoMap.remove(ctx);
		contextToPlayerInfoMap.remove(ctx);
		if (info != null) {
			idToContextMap.remove(info.id);
			System.err.println(info.handle + " disconnected");
			endedConnectionNotification.add(info);
		}
	}
	
	private void receiveMove(PlayerInfo player, int x, int y) {
		movePlayer(player);
		playerTargetLocations.put(player, PlayerAction.move(new Vec3(x, y, currentTime())));
		changedLocations.add(player.id);
		sendLocations(false);
	}
	
	private void receiveTunnelCollapse(PlayerInfo player, JSONObject collapseObj) {
		int nodeid1 = collapseObj.getInt("nodeid1");
		int nodeid2 = collapseObj.getInt("nodeid2");

		if (!state.doesTunnelNodeExist(nodeid1) || !state.doesTunnelNodeExist(nodeid2)) {
			System.err.println("ERROR COLLAPSING TUNNEL: node1 or node2 doesnt exist");
			return;
		}
		
		Vec3 playerPos = getInterpolatedPlayerPosition(player);
		Vec2 nodePos = state.getTunnelNodePosition(nodeid1);
		double distance = nodePos.distanceTo(playerPos.xy());
		if (distance > TUNNEL_SIZE) {
			System.err.println("ERROR COLLAPSING TUNNEL: player too far from node1");
			return;
		}
		
		Set<TunnelSegment> affectedSegments = state.getTunnelSegmentFrom(nodeid1, player.id);
		TunnelSegment collapsingSegment = null;
		for (TunnelSegment segment : affectedSegments) {
			if (segment.node1 == nodeid2 || segment.node2 == nodeid2) {
				collapsingSegment = segment;
			}
		}
		if (collapsingSegment == null) {
			System.err.println("ERROR COLLAPSING TUNNEL: no segment between specified nodes");
			return;
		}
		
		if (affectedSegments.size() > 1) {
			// need to make copy of node and update collapsing segment to use it
			TunnelNode copyNode = state.createNewTunnelNode(nodePos.x, nodePos.y, player.id);
			if (collapsingSegment.node1 == nodeid1) {
				collapsingSegment.node1 = copyNode.id;
			}
			else {
				collapsingSegment.node2 = copyNode.id;
			}
			nodeid1 = copyNode.id;
			state.updateTunnelSegment(collapsingSegment);
			JSONObject obj = new JSONObject().put("type", MessageType.TUNNEL)
					.put("tunnelnodes", new JSONArray().put(new JSONObject(copyNode)))
					.put("tunnelsegments", new JSONArray().put(new JSONObject(collapsingSegment)));
			sendToOne(obj.toString(), idToContextMap.get(player.id));
		}
		
		Vec2 targetPos = state.getTunnelNodePosition(nodeid2);
		movePlayer(player);
		playerTargetLocations.put(player, PlayerAction.collapse(
				new Vec3(targetPos.x, targetPos.y, currentTime()), 
				nodeid1,
				collapsingSegment));
		changedLocations.add(player.id);
		sendLocations(false);
	}
	
	private Rectangle getRoomForPoint(int x, int y) {
		Point p = new Point(x, y);
		for (Rectangle room : mapRooms) {
			if (room.contains(p)) {
				return room;
			}
		}
		return null;
	}
	
	private void receiveTunnel(PlayerInfo player, JSONObject message) {
		if (message.has("collapse")) {
			receiveTunnelCollapse(player, message.getJSONObject("collapse"));
			return;
		}
		Rectangle node1Room = null;
		int nodeid1 = -1;
		if (message.has("nodeid1")) {
			nodeid1 = message.getInt("nodeid1");
		}
		else {
			int x = message.getInt("x1");
			int y = message.getInt("y1");
			node1Room = getRoomForPoint(x, y);
			if (node1Room == null) {
				System.out.println("ERROR MAKING TUNNEL: node1 outside of room");
				return;
			}
			TunnelNode node = state.createNewTunnelNode(x, y, player.id);
			nodeid1 = node.id;
		}
		if (!state.doesTunnelNodeExist(nodeid1)) {
			System.err.println("ERROR MAKING TUNNEL");
		}
		Vec2 node1Pos = state.getTunnelNodePosition(nodeid1);
		if (node1Room == null) {
			node1Room = getRoomForPoint(node1Pos.x, node1Pos.y);
		}
		
		Vec2 node2Pos = null;
		if (message.has("nodeid2") && state.doesTunnelNodeExist(message.getInt("nodeid2"))) {
			node2Pos = state.getTunnelNodePosition(message.getInt("nodeid2"));
		}
		else {
			node2Pos = new Vec2(message.getInt("x2"), message.getInt("y2"));
		}

		int nodeid2 = -1;
		TunnelNode createdNode2 = null;
		double proposedLength = node1Pos.distanceTo(node2Pos);
		int maxLength = Constants.getMaxSegmentLength(player._getTunnelingLevel());
		
		if (proposedLength > maxLength) {
			System.err.println("ERROR DIGGING TUNNEL: too long " + proposedLength);
			return;
//			Vec2 tunnelVector = node2Pos.minus(node1Pos);
//			Vec2 croppedVector = tunnelVector.multiply(maxLength / tunnelVector.magnitude());
//			node2Pos = node1Pos.add(croppedVector);
//			
//			TunnelNode node = state.createNewTunnelNode(node2Pos.x, node2Pos.y, player.id);
//			nodeid2 = node.id;
		}
		
		// both nodes in same room == cheaper
		int cost = 100 + (int)(Math.sqrt(proposedLength / 10));
		if (node1Room == null || !node1Room.contains(new Point(node2Pos.x, node2Pos.y))) {
			cost += 10 * state.getPlayerTunnels(player.id).size();
		}
		
		if (player.numcoins < cost) {
			System.out.println("ERROR MAKING TUNNEL: not enough funds");
			return;
		}
		
		createdNode2 = state.createNewTunnelNode(node1Pos.x, node1Pos.y, player.id);
		if (message.has("nodeid2")) {
			nodeid2 = message.getInt("nodeid2");
		}
		else {
			nodeid2 = createdNode2.id;
		}
		if (!state.doesTunnelNodeExist(nodeid2)) {
			System.err.println("ERROR MAKING TUNNEL");
			return;
		}
		
		player.numcoins -= cost;
		state.updatePlayerInfo(player);
		
		TunnelSegment segment = state.createNewTunnelSegment(nodeid1, createdNode2.id, player.id);
		
		JSONArray nodeArr = new JSONArray()
				.put(new JSONObject(state.getTunnelNode(nodeid1)))
				.put(new JSONObject(createdNode2));
		JSONObject obj = new JSONObject().put("type", MessageType.TUNNEL);
		obj.put("tunnelnodes", nodeArr);
		obj.put("tunnelsegments", new JSONArray().put(new JSONObject(segment)));
		sendToOne(obj.toString(), idToContextMap.get(player.id));
		
		movePlayer(player);
		playerTargetLocations.put(player, PlayerAction.dig(
				new Vec3(node2Pos.x, node2Pos.y, currentTime()), 
				createdNode2.id,
				nodeid2,
				segment));
		changedLocations.add(player.id);
		sendLocations(false);
	}
	
	private void unlockSkill(PlayerInfo player, String skill) {
		if (skill.equals("tunneling")) {
			state.playerUnlocksTunneling(player);
		}
	}
	
	private void teleport(PlayerInfo player, String target) {
		if (target.equals("home")) {
			playerTargetLocations.remove(player);
			player.x = 0;
			player.y = 0;
			changedLocations.add(player.id);
			sendLocations(false);
		}
	}
	
	private void receiveTestMessage(PlayerInfo player, JSONObject obj) {
		System.out.println(obj.toString());
		if (obj.has("setTunnelingExp")) {
			try {
				int newTunnelingExp = Integer.parseInt(obj.getString("setTunnelingExp"));
				player.tunnelingExp = newTunnelingExp;
				state.updatePlayerInfo(player);
				changedLocations.add(player.id);
			}
			catch (NumberFormatException e) {
				
			}
		}
	}
	
	public void receiveMessage(WebSocketSession ctx, TextMessage textMessage) {
		String message = textMessage.getPayload();
		JSONObject obj = new JSONObject(message); 
		MessageType type = MessageType.valueOf(obj.getString("type"));
		
		if (type == MessageType.HELLO) {
			newConnection(ctx, obj.getString("session"));
			return;
		}
		
		PlayerInfo player = contextToPlayerInfoMap.get(ctx);
		if (player == null) {
			return;
		}
		
		switch(type) {
		case MOVE:
			receiveMove(player, obj.getInt("x"), obj.getInt("y"));
			break;

		case STOP:
			stopGame(player);
			break;
		
		case TUNNEL:
			receiveTunnel(player, obj);
			break;
		
		case UNLOCK:
			unlockSkill(player, obj.getString("skill"));
			break;
			
		case TELEPORT:
			teleport(player, obj.getString("target"));
			break;
			
		case TEST:
			receiveTestMessage(player, obj);
			break;
			
		default:
			System.err.println("UNKNOWN MESSAGE TYPE RECEIVED: " + type);
		}
	}
	
	private java.util.List<Rectangle> mapRooms = new ArrayList<>();
	// TODO cleanup later
	{ 
		mapRooms.add(new Rectangle(-10000, -10000, 20000, 20000));
		mapRooms.add(new Rectangle( 25000, -20000, 10000, 10000));
		mapRooms.add(new Rectangle( 25000, -5000, 10000, 10000));
		mapRooms.add(new Rectangle( 25000, 10000, 10000, 10000));

//		mapRooms.add(new Rectangle(-25000, -26000, 40000, 1000));
//		mapRooms.add(new Rectangle(-25000, +25000, 40000, 1000));
	}
	
	private Vec2 tryToMovePlayerToward(PlayerInfo player, Vec2 target) {

		Vec2 playerVec = new Vec2(player.x, player.y);
		boolean validMove = false;
		if (playerTargetLocations.get(player).type == PlayerActionType.DIG) {
			validMove = true;
		}
		if (!validMove) {
			for (Rectangle room : mapRooms) {
				if (room.contains(new Point(target.x, target.y))) {
					validMove = true;
					break;
				}
			}
		}
		if (!validMove) {
			// If outside of valid room, see if currently in a tunnel
			boolean inTunnel = false;
			for (TunnelSegment tunnel : state.getPlayerTunnels(player.id)) {
				
				Vec2 tunnelStart = state.getTunnelNodePosition(tunnel.node1);//new Vec2(tunnel.node1.x, tunnel.node1.y);
				Vec2 tunnelEnd = state.getTunnelNodePosition(tunnel.node2); //new Vec2(tunnel.node2.x, tunnel.node2.y)/*; semicolon sus ඞ*/;
				Vec2 tunnelVec = tunnelEnd.minus(tunnelStart);
				
				double tunnelLength = tunnelEnd.minus(tunnelStart).magnitude();
				
				double distanceAlongTunnel = VectorMath.projectPointOntoLine(target, tunnelStart, tunnelEnd);
				Vec2 projectedTarget = tunnelStart.add(tunnelVec.multiply(distanceAlongTunnel/tunnelVec.magnitude()));
				double distanceToTunnel = projectedTarget.distanceTo(target);
				
//				System.out.println("distanceAlongTunnel: " + distanceAlongTunnel);
//				System.out.println("distanceToTunnel: " + distanceToTunnel);
//				System.out.println("projectedTarget: " + projectedTarget);
//				System.out.println("tunnel: " + tunnelStart + " -> " + tunnelEnd);
				
				if (distanceAlongTunnel >= -60 && distanceAlongTunnel <= tunnelLength + 60) {
					// within tunnel length
					if (distanceToTunnel <= 220) {
						// close enough to tunnel
						inTunnel = true;
						break;
					}
					else if (distanceToTunnel <= TUNNEL_SIZE) {
						double currentDistanceAlongTunnel = VectorMath.projectPointOntoLine(playerVec, tunnelStart, tunnelEnd);
						Vec2 currentProjection = tunnelStart.add(tunnelEnd.minus(tunnelStart).multiply(currentDistanceAlongTunnel));
						double currentDistanceToTunnel = currentProjection.distanceTo(playerVec);
						if (distanceToTunnel < currentDistanceToTunnel) {
							// allow movement back towards in bounds
							inTunnel = true;
							break;
						}
					}
				}
				else if (distanceAlongTunnel >= -100 && distanceAlongTunnel <= tunnelLength + 100) {
					// outside of tunnel length
					double currentDistanceAlongTunnel = VectorMath.projectPointOntoLine(playerVec, tunnelStart, tunnelEnd);
					if (distanceAlongTunnel < currentDistanceAlongTunnel) {
						// allow movement back towards tunnel
						inTunnel = true;
						break;
					}
				}
			}
			if (!inTunnel) {
//				return new Vec2(player.x, player.y);
				return null;
			}
		}
		return target;
	}
	
	private void movePlayer(PlayerInfo player) {
		if (!playerTargetLocations.containsKey(player)) {
			return;
		}
		Vec3 interpolatedPosition = getInterpolatedPlayerPosition(player);
		Vec2 targetPosition = interpolatedPosition.xy();
		
		Vec2 resultPosition = tryToMovePlayerToward(player, targetPosition);
		if (resultPosition == null) {
			playerTargetLocations.remove(player);
			changedLocations.add(player.id);
			sendLocations(false);
			return;
		}
		
		// TODO add check if player actually moved here.
		// if no movement, send info to client
		Vec2 previousPosition = new Vec2(player.x, player.y);
		player.x = resultPosition.x;
		player.y = resultPosition.y;

		PlayerAction action = playerTargetLocations.get(player);;
		Vec3 fullTarget = action.targetPosition;
		
		if(interpolatedPosition == fullTarget) {
			playerTargetLocations.remove(player);
			System.out.println(player.id + " reached destination");
		}
		else {
			fullTarget.z = currentTime();
		}
		
		

		JSONArray nodesArr = new JSONArray();
		JSONArray segmentsArr = new JSONArray();
		if (action.type == PlayerActionType.COLLAPSE || action.type == PlayerActionType.DIG) {
			TunnelNode node = state.updateTunnelNodePosition(action.nodeid, resultPosition);
			nodesArr.put(new JSONObject(node));
			double distanceMoved = previousPosition.distanceTo(resultPosition);
			state.playerGainsExp(player, (int)(distanceMoved/100));
		}
		if(interpolatedPosition == fullTarget) {
			// TODO resolve collapse,dig actions
			if (action.type == PlayerActionType.DIG) {
				if (action.targetNodeId != -1 && action.segment != null && action.targetNodeId != action.nodeid) {
					if (action.segment.node1 == action.nodeid) {
						action.segment.node1 = action.targetNodeId;
					}
					else if (action.segment.node2 == action.nodeid) {
						action.segment.node2 = action.targetNodeId;
					}
					state.updateTunnelSegment(action.segment);
					state.deleteNode(action.nodeid);
					nodesArr.put(new JSONObject(state.getTunnelNode(action.nodeid)).put("delete", true));
					segmentsArr.put(new JSONObject(action.segment));
				}
			}
			else if (action.type == PlayerActionType.COLLAPSE) {
				segmentsArr.put(new JSONObject(action.segment).put("delete", true));
				nodesArr.put(new JSONObject(state.getTunnelNode(action.nodeid)).put("delete", true));
				int remainingNodeId = (action.segment.node1 == action.nodeid) ? action.segment.node2 : action.segment.node1;
				Set<TunnelSegment> segmentsAttachedToRemainingNode = state.getTunnelSegmentFrom(remainingNodeId, player.id);
				state.collapseTunnelSegment(action.segment.id, player.id);
				state.deleteNode(action.nodeid);
				if (segmentsAttachedToRemainingNode.size() == 1) {
					nodesArr.put(new JSONObject(state.getTunnelNode(remainingNodeId)).put("delete", true));
					state.deleteNode(remainingNodeId);
				}
			}
		}
		if (nodesArr.length() > 0 || segmentsArr.length() > 0) {
			JSONObject obj = new JSONObject()
					.put("type", MessageType.TUNNEL)
					.put("tunnelnodes", nodesArr)
					.put("tunnelsegments", segmentsArr);
			sendToOne(obj.toString(), idToContextMap.get(player.id));
		}
		
		changedLocations.add(player.id);
		state.updatePlayerInfo(player);
	}
	
	private Vec3 getInterpolatedPlayerPosition(PlayerInfo player) {
		if (!playerTargetLocations.containsKey(player)) {
			return new Vec3(player.x, player.y, 0);
		}
		PlayerAction action = playerTargetLocations.get(player);;
		Vec3 fullTarget = action.targetPosition;
		Vec2 targetLocation = fullTarget.xy();
		int elapsedTime = currentTime() - fullTarget.z;
		
		Vec2 deltaLocation = new Vec2(targetLocation.x - player.x, targetLocation.y - player.y);
		double distanceLeft = deltaLocation.magnitude();
		
		double distanceTravelled = elapsedTime * PLAYER_SPEED / 1000.0;
		if (distanceTravelled >= distanceLeft) {
			return fullTarget;
		}
		else {
			deltaLocation.scale(distanceTravelled/distanceLeft);
			return new Vec3(player.x + deltaLocation.x, player.y + deltaLocation.y, 0);
		}
	}
	
	private void movePlayers() {
		for (PlayerInfo info : contextToPlayerInfoMap.values()) {
			movePlayer(info);
		}
	}
	
	private void checkCoinCollect() {
		for (PlayerInfo info : contextToPlayerInfoMap.values()) {
			List<Coin> coins = state.getCoinsInRange(
					new Vec2(info.x - 300, info.y - 300), 
					new Vec2(info.x + 300, info.y + 300));
//			System.err.println("close to " + coins.size() + " coins");
			for(Coin c : coins) {
				state.playerCollectsCoin(info, c);
				deletedCoins.add(c);
			}
		}
		if (!deletedCoins.isEmpty()) {
			sendLocations(false);
		}
	}
	
	private Vec2 getPosForCoinInRoom(Rectangle room, double spawnRangeMultiplier) {
		int centerx = room.x + room.width/2;
		int offsetx = (int) (Util.gaussian() * room.width * spawnRangeMultiplier / 2);
		int coinx = centerx + offsetx;
		
		int centery = room.y + room.height/2;
		int offsety = (int) (Util.gaussian() * room.height * spawnRangeMultiplier / 2);
		int coiny = centery + offsety;
		return new Vec2(coinx, coiny);
	}
	
	private int getValueForCoin(int min, int max) {
		return min + (int)(Math.abs(Util.gaussian()) * (max - min));
	}

	private void addCoin() {
		Vec2 room0pos = getPosForCoinInRoom(mapRooms.get(0), 1.2);
		int room0value = getValueForCoin(1, 10);
		
		Coin coin = state.addNewCoin(room0pos.x, room0pos.y, room0value);
		newCoins.add(coin);
		
		if (Math.random() < 0.4) {
			Rectangle room1 = mapRooms.get(1 + (int)(Math.random() * (mapRooms.size() - 1)));
			Vec2 room1pos = getPosForCoinInRoom(room1, 1.2);
			int room1value = getValueForCoin(5, 20);

			if (Math.random() < 0.001) {
				room1pos = getPosForCoinInRoom(room1, 1.3);
				room1value = getValueForCoin(80, 100);
			}
			Coin coin1 = state.addNewCoin(room1pos.x, room1pos.y, room1value);
			newCoins.add(coin1);
		}
	}
	
	private void gameFunction() {
		try {
			long timeToNextCoin = currentTime();
			while(!stopGame) {
				long starttime = currentTime();
				movePlayers();
				checkCoinCollect();
				
				if (currentTime() - timeToNextCoin >= 0 && state.getCoins().size() < 3000) {
					addCoin();
					timeToNextCoin = currentTime() + TICK_TIME;
					timeToNextCoin += Math.max(0, 20000 - TICK_TIME*contextToPlayerInfoMap.size());
				}
//				System.out.println("game tick: " + currentTime()%10000);
				long deltatime = currentTime() - starttime;
				long timetosleep = TICK_TIME - deltatime;
				if (timetosleep > 0) {
					Thread.sleep(timetosleep);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.err.println("finished gameFunction");
	}

	private volatile int lastSentUpdate;
	private void updateFunction() {
		try {
			while(!stopGame) {
				if (currentTime() >= lastSentUpdate + UPDATE_TIME) {
					sendLocations(true);
				}
				long timetosleep = UPDATE_TIME + lastSentUpdate - currentTime();
				Thread.sleep(timetosleep);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.err.println("finished updateFunction");
	}
	private void sendLocations(boolean heartbeat) {
		lastSentUpdate = currentTime();
		// TODO add an output queue intermediate here?
		JSONObject jo = new JSONObject();
		jo.put("type", MessageType.MOVE);
		
		JSONArray players = new JSONArray();
		for (PlayerInfo info : contextToPlayerInfoMap.values()) {
			if (changedLocations.contains(info.id) || sendAllPlayerLocations) {
				changedLocations.remove(info.id);
				
				Vec3 location = getInterpolatedPlayerPosition(info);
				JSONObject obj = new JSONObject(info);
				obj.put("x", location.x);
				obj.put("y", location.y);
				
				if (playerTargetLocations.containsKey(info)) {
					obj.put("target", new JSONObject(playerTargetLocations.get(info).targetPosition));
				}
				players.put(obj);
			}
		}
		if (players.length() > 0)
			jo.put("players", players);

		sendAllPlayerLocations = false;

		JSONArray coinsArray = new JSONArray();
		while(!newCoins.isEmpty()) {
			coinsArray.put(new JSONObject(newCoins.remove()));
		}
		while(!deletedCoins.isEmpty()) {
			coinsArray.put(new JSONObject(deletedCoins.remove()).put("delete", true));
		}
		
		for (WebSocketSession ctx : contextToPlayerInfoMap.keySet()) {
			// TODO share all coins on connect then only send new coins to all connections
			// that way no need to keep list of shared coins per connection
			// also no need to iterate through entire list of coins per connection

			if (coinsArray.length() > 0) {
				jo.put("coins", coinsArray);
			}
			
			// if at least 1 thing to send
			// TODO heartbeat needs to be recorded per connection. 
			// if I start sending only data that is in viewport,
			// then need to make sure that all connections get periodic heartbeats.
			if (heartbeat || jo.length() > 1) {
				sendToOne(jo.toString(), ctx);
			}
		}
		
		if (!newConnectionNotification.isEmpty()) {
			JSONObject playerMapping = new JSONObject();
			while (!newConnectionNotification.isEmpty()) {
				AccountInfo accountInfo = newConnectionNotification.remove();
				playerMapping.put("" + accountInfo.id, accountInfo.handle);
			}
			playerMapping.put("type", MessageType.MAPPING);
			sendToAll(playerMapping.toString());
		}

		
		if (!endedConnectionNotification.isEmpty()) {
			JSONArray disconnected = new JSONArray();
			while (!endedConnectionNotification.isEmpty()) {
				AccountInfo accountInfo = endedConnectionNotification.remove();
				disconnected.put(accountInfo.id);
			}
			JSONObject obj = new JSONObject()
					.put("type", MessageType.DC)
					.put("ids", disconnected);
			sendToAll(obj.toString());
		}
	}
	
	private void sendToAll(String message) {
		TextMessage textMessage = new TextMessage(message);
		for (WebSocketSession ctx : contextToPlayerInfoMap.keySet()) {
			sendToOne(textMessage, ctx);
		}
	}

	private void sendToOne(String message, WebSocketSession ctx) {
		sendToOne(new TextMessage(message), ctx);
	}
	private void sendToOne(TextMessage message, WebSocketSession ctx) {
		if (ctx.isOpen()) {
			try {
				synchronized(ctx) {
					ctx.sendMessage(message);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void shareAllPlayerMappingWith(WebSocketSession ctx) {
		JSONObject playerMapping = new JSONObject();
		for (AccountInfo accountInfo : contextToAccountInfoMap.values()) {
			playerMapping.put("" + accountInfo.id, accountInfo.handle);
		}
		playerMapping.put("type", MessageType.MAPPING);
		sendToOne(playerMapping.toString(), ctx);
	}
	
	private void databaseSaveFunction() {
		try {
			while(!stopGame) {
				state.persist();
				for(int i = 0; i < 10000/UPDATE_TIME && !stopGame; i++) {
					Thread.sleep(UPDATE_TIME);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.err.println("finished databaseSaveFunction");
	
	}
	
	private void sendBye(WebSocketSession ctx, String message) {
		JSONObject obj = new JSONObject()
				.put("type", MessageType.BYE)
				.put("message", message);
		sendToOne(obj.toString(), ctx);
	}
	
	@Override
	public String toString() {
		return "coingame{" + String.join(", ", new String[]
				{
					"#connections: " + contextToPlayerInfoMap.size(),
					"stopGame: " + stopGame
				}) + "}";
	}
}
