package games.coingame;

import java.beans.JavaBean;

@JavaBean
public class Coin {

	private static int idcounter = 0;
	public int id;
	public int x;
	public int y;
	public Coin(int id, int x, int y) {
		this.id = id;
		this.x = x;
		this.y = y;
	}
	
	public static Coin makeCoin(int x, int y) {
		return new Coin(idcounter++, x, y);
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
	
	
	
}
