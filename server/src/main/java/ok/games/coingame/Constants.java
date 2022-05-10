package ok.games.coingame;

public class Constants {

	public static final int UNLOCK_TUNNELING_COST = 500;
	public static final int CROWN_COST = 10000;

	
	public static final int getLevelFromExperience(int exp) {
		if (exp <= 0) {
			return 0;
		}
		else if (exp >= 969819) {
			return 99;
		}
		else {
			return (int)(14*Math.log(exp + 885) - 94);
		}
	}
	
	public static final int getMaxSegmentLength(int tunnelingLevel) {
		return 3000 + 50*tunnelingLevel;
	}
}
