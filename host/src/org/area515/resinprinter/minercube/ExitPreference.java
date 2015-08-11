package org.area515.resinprinter.minercube;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;


public enum ExitPreference {
	SAME_FACE    (
			new Direction[]{Direction.xPositive},
			new Direction[]{Direction.xNegative},
			new Direction[]{Direction.yPositive},
			new Direction[]{Direction.yNegative},
			new Direction[]{Direction.zPositive},
			new Direction[]{Direction.zNegative}),
	OPPOSITE_FACE(
			new Direction[]{Direction.xNegative},
			new Direction[]{Direction.xPositive},
			new Direction[]{Direction.yNegative},
			new Direction[]{Direction.yPositive},
			new Direction[]{Direction.zNegative},
			new Direction[]{Direction.zPositive}),
	ANY_FACE(
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative}),
	ADJACENT_FACE(
			new Direction[]{Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative}),
	ANY_FACE_OTHER_THAN_ENTER_FACE(
			new Direction[]{Direction.xNegative,Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.yPositive,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yNegative,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.zPositive,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative,Direction.zNegative},
			new Direction[]{Direction.xPositive,Direction.xNegative,Direction.yPositive,Direction.yNegative,Direction.zPositive});

	LinkedHashMap<Direction, Direction[]> exitDirections = new LinkedHashMap<Direction, Direction[]>();
	
	ExitPreference(
			Direction xPositiveExitDirections[], 
			Direction xNegativeExitDirections[],
			Direction yPositiveExitDirections[],
			Direction yNegativeExitDirections[],
			Direction zPositiveExitDirections[],
			Direction zNegativeExitDirections[]) {
		exitDirections.put(Direction.xPositive, xPositiveExitDirections);
		exitDirections.put(Direction.xNegative, xNegativeExitDirections);
		exitDirections.put(Direction.yPositive, yPositiveExitDirections);
		exitDirections.put(Direction.yNegative, yNegativeExitDirections);
		exitDirections.put(Direction.zPositive, zPositiveExitDirections);
		exitDirections.put(Direction.zNegative, zNegativeExitDirections);
	}
	
	public List<Direction> getPossibleExitDirections(Direction direction) {
		return Arrays.asList(exitDirections.get(direction));
	}
}