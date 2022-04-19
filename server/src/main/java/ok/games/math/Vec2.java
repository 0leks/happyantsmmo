package ok.games.math;

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
		return Math.sqrt(dot(this));
	}
	public Vec2 minus(Vec2 other) {
		return new Vec2(this.x - other.x, this.y - other.y);
	}
	public Vec2 add(Vec2 other) {
		return new Vec2(this.x + other.x, this.y + other.y);
	}
	public double dot(Vec2 other) {
		return this.x*other.x + this.y*other.y;
	}
	public Vec2 multiply(double value) {
		return new Vec2((int)(x*value), (int)(y*value));
	}
	public double distanceTo(Vec2 other) {
		return Math.sqrt( (x - other.x) * (x - other.x) + (y - other.y) * (y - other.y) );
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
	
	@Override
	public String toString() {
		return String.format("<%d, %d>", x, y);
	}
}
