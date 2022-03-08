package games.math;

import java.beans.JavaBean;

@JavaBean
public class Vec2 {
	public int x;
	public int y;
	public Vec2(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public void scale(double ratio) {
		x = (int)(x*ratio);
		y = (int)(y*ratio);
	}
	public double magnitude() {
		return Math.sqrt(x*x + y*y);
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
