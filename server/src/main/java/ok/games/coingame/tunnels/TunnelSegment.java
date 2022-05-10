package ok.games.coingame.tunnels;

import java.beans.JavaBean;

import ok.games.math.Vec2;

@JavaBean
public class TunnelSegment {

	public int id;
	public int node1;
	public int node2;
	public int playerid;
	public TunnelSegment(int id, int node1, int node2, int playerid) {
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
	public int getNode1() {
		return node1;
	}
	public void setNode1(int node1) {
		this.node1 = node1;
	}
	public int getNode2() {
		return node2;
	}
	public void setNode2(int node2) {
		this.node2 = node2;
	}
	public int getPlayerid() {
		return playerid;
	}
	public void setPlayerid(int playerid) {
		this.playerid = playerid;
	}
	
	
}
