package ok.test;

import ok.games.coingame.Constants;

public class LevelingTest {
	
	
	private static final void testLevelThresholds() {
		System.out.println("TESTING LEVEL THRESHOLDS");
		int previousLevel = -1;
		int previousExp = 0;
		for (int exp = 0; exp < 1000000; exp += 1) {
			double level = Constants.getLevelFromExperience(exp);
			int roundedLevel = (int)level;
			
			if (roundedLevel > previousLevel) {
				int deltaExp = exp - previousExp;
				System.out.println(String.format("%6d (+%5d) -> %.00f", exp, deltaExp, level));
				previousLevel = roundedLevel;
				previousExp = exp;
			}
		}
	}
	
	private static final void testLevelPercent() {
		System.out.println("TESTING LEVEL PERCENTS");
		double previousLevel = 10;
		for (int exp = 859; exp < 984; exp += 1) {
			double level = Constants.getLevelFromExperience(exp);
			double delta = level - previousLevel;
			System.out.println(String.format("%.04f   +%f", level, delta));
			previousLevel = level;
		}
	}
	
	private static final int[] getExperienceTable() {
		int[] table = new int[100];
		int previousLevel = -1;
		for (int exp = 0; exp < 1000000; exp += 1) {
			int level = Constants.getLevelFromExperience(exp);
			if (level > previousLevel) {
				table[level] = exp;
				previousLevel = level;
			}
		}
		return table;
	}
	
	private static final void printExpTable() {
		int[] expTable = getExperienceTable();
		StringBuilder sb = new StringBuilder();
		for(int level = 0; level < expTable.length; ++level) {
			sb.append("" + expTable[level]);
			if (level != expTable.length-1) {
				sb.append(",");
			}
		}
		System.out.println(sb.toString());
	}
	
	public static void main(String[] args) {
		testLevelThresholds();
//		testLevelPercent();
		printExpTable();
	}
}
