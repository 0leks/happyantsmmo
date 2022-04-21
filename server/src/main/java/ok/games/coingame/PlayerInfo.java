package ok.games.coingame;

import java.beans.JavaBean;

@JavaBean
public class PlayerInfo {
	
	public int id;
	public int numcoins;
	public int x;
	public int y;
	public int tunnelingLevel;

	public PlayerInfo(int id, int numcoins, int x, int y, int tunnelingLevel) {
		this.id = id;
		this.numcoins = numcoins;
		this.x = x;
		this.y = y;
		this.tunnelingLevel = tunnelingLevel;
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
	public int getTunnelingLevel() {
		return tunnelingLevel;
	}
	public void setTunnelingLevel(int tunnelingLevel) {
		this.tunnelingLevel = tunnelingLevel;
	}

	@Override
	public String toString() {
		return String.format("{%d, %d, %d, %d, %d}", id, numcoins, x, y, tunnelingLevel);
	}
}
