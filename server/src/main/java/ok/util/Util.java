package ok.util;

import ok.games.math.Vec2;

public class Util {
	private static final long startTime = System.currentTimeMillis();
	public static final int currentTime() {
		return (int)(System.currentTimeMillis() - startTime);
	}
	
	/**
	 * 
	 * @return random number between -1 and 1
	 */
	public static final double gaussian() {
		return (Math.random() + Math.random() + Math.random() + Math.random()) / 2 - 1;
	}
	
	public static final double reverseGaussian() {
		double g = gaussian();
		if (g < 0) {
			return -1 - g;
		}
		else {
			return 1 - g;
		}
	}
	
	public static final double distanceToLine(Vec2 point, Vec2 line1, Vec2 line2) {
		double a = (line1.y - line2.y);
		double b = (line2.x - line1.x);
		double c = -line1.x * a - line1.y * b;
		
		double d = Math.abs(a * point.x + b * point.y + c) / Math.sqrt(a*a + b*b);
		return d;
	}
}
