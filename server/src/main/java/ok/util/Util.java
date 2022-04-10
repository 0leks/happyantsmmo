package ok.util;

public class Util {
	private static final long startTime = System.currentTimeMillis();
	public static final int currentTime() {
		return (int)(System.currentTimeMillis() - startTime);
	}
	
	public static final double gaussian() {
		return (Math.random() + Math.random() + Math.random() + Math.random()) / 4;
	}
	
	public static final double reverseGaussian() {
		double g = gaussian();
		if (g < 0.5) {
			return 0.5 - g;
		}
		else {
			return 1.5 - g;
		}
	}
}
