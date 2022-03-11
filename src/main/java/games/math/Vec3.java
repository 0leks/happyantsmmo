package games.math;

import java.beans.JavaBean;

@JavaBean
public class Vec3 {
	public int x;
	public int y;
	public int z;
	public Vec3(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vec2 xy() {
		return new Vec2(x, y);
	}
	
	public void scale(double ratio) {
		x = (int)(x*ratio);
		y = (int)(y*ratio);
		z = (int)(z*ratio);
	}
	public double magnitude() {
		return Math.sqrt(x*x + y*y + z*z);
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
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
}
