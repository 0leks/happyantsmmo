package ok.games.coingame.tunnels;

import java.beans.JavaBean;

import ok.games.math.Vec2;

@JavaBean
public class TunnelSegment {

	public int id;
	public TunnelNode node1;
	public TunnelNode node2;
	public int playerid;
	public TunnelSegment(int id, TunnelNode node1, TunnelNode node2, int playerid) {
		this.id = id;
		this.node1 = node1;
		this.node2 = node2;
		this.playerid = playerid;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public TunnelNode getNode1() {
		return node1;
	}
	public void setNode1(TunnelNode node1) {
		this.node1 = node1;
	}
	public TunnelNode getNode2() {
		return node2;
	}
	public void setNode2(TunnelNode node2) {
		this.node2 = node2;
	}
	public int getPlayerid() {
		return playerid;
	}
	public void setPlayerid(int playerid) {
		this.playerid = playerid;
	}
	
	public double length() {
		return new Vec2(node1.x, node1.y).distanceTo(new Vec2(node2.x, node2.y));
	}
	
	
}
