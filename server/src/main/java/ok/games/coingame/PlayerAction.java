package ok.games.coingame;

import ok.games.math.Vec3;

import static ok.games.coingame.PlayerActionType.*;

import ok.games.coingame.tunnels.TunnelSegment;

public class PlayerAction {
	public PlayerActionType type;
	public Vec3 targetPosition;
	public int nodeid;
	public TunnelSegment segment;
	public int targetNodeId;
	private PlayerAction(PlayerActionType type, Vec3 targetPosition, int nodeid, int targetNodeId, TunnelSegment segment) {
		this.type = type;
		this.targetPosition = targetPosition;
		this.nodeid = nodeid;
		this.targetNodeId = targetNodeId;
		this.segment = segment;
	}
	
	public static PlayerAction move(Vec3 targetPosition) {
		return new PlayerAction(MOVE, targetPosition, -1, -1, null);
	}
	
	public static PlayerAction collapse(Vec3 targetPosition, int nodeid, TunnelSegment segment) {
		return new PlayerAction(COLLAPSE, targetPosition, nodeid, -1, segment);
	}
	
	public static PlayerAction dig(Vec3 targetPosition, int nodeid, int targetNodeId, TunnelSegment segment) {
		return new PlayerAction(DIG, targetPosition, nodeid, targetNodeId, segment);
	}
}
