package ok.games.coingame.tunnels;

import java.beans.JavaBean;

@JavaBean
public class TunnelNode {
	
	public int id;
	public int x;
	public int y;
	public int playerid;
	
	public TunnelNode(int id, int x, int y, int playerid) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.playerid = playerid;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}

	public int getPlayerid() {
		return playerid;
	}

	public void setPlayerid(int playerid) {
		this.playerid = playerid;
	}
	
	
}
