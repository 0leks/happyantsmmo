package ok.games.coingame;

import java.beans.JavaBean;

@JavaBean
public class Coin {

	public int id;
	public int x;
	public int y;
	public int value;
	public Coin(int id, int x, int y, int value) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.value = value;
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
	
	public int getValue() {
		return value;
	}
	public void setValue() {
		this.value = value;
	}
}
