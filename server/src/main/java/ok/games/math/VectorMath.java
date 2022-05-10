package ok.games.math;

public class VectorMath {

	
	public static double projectPointOntoLine(Vec2 point, Vec2 lineStart, Vec2 lineVector) {
		Vec2 A = point.minus(lineStart);
		Vec2 B = lineVector.minus(lineStart);
		
		double adjacentLenth = A.dot(B) / B.magnitude();
		
		return adjacentLenth;
	}
}
