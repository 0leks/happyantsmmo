package ok.util;

public class Util {
	private static final long startTime = System.currentTimeMillis();
	public static final int currentTime() {
		return (int)(System.currentTimeMillis() - startTime);
	}
}
