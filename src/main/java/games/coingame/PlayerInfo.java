package games.coingame;

import java.beans.JavaBean;

@JavaBean
public class PlayerInfo {
	
	public int id;
	public int numcoins;
	public int x;
	public int y;

	public PlayerInfo(int id, int numcoins, int x, int y) {
		this.id = id;
		this.numcoins = numcoins;
		this.x = x;
		this.y = y;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public int getNumcoins() {
		return numcoins;
	}

	public void setNumcoins(int numcoins) {
		this.numcoins = numcoins;
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
}
