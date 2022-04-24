package ok.games.coingame;

import ok.games.math.Vec3;

import static ok.games.coingame.PlayerActionType.*;

public class PlayerAction {
	public PlayerActionType type;
	public Vec3 targetPosition;
	public int nodeid;
	private PlayerAction(PlayerActionType type, Vec3 targetPosition, int nodeid) {
		this.type = type;
		this.targetPosition = targetPosition;
		this.nodeid = nodeid;
	}
	
	public static PlayerAction move(Vec3 targetPosition) {
		return new PlayerAction(MOVE, targetPosition, -1);
	}
	
	public static PlayerAction collapse(Vec3 targetPosition, int nodeid) {
		return new PlayerAction(COLLAPSE, targetPosition, nodeid);
	}
}
