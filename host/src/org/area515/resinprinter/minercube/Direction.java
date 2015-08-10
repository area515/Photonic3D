package org.area515.resinprinter.minercube;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public enum Direction {
	xPositive(1, 0, 0, ">"),
	xNegative(-1, 0, 0, "<"),
	yPositive(0, 1, 0, "V"),
	yNegative(0, -1, 0, "^"),
	zPositive(0, 0, 1, "U"),
	zNegative(0, 0, -1, "D");
	
	static {
		Direction.xPositive.setOppositeDirection(Direction.xNegative);
		Direction.xNegative.setOppositeDirection(Direction.xPositive);
		Direction.yPositive.setOppositeDirection(Direction.yNegative);
		Direction.yNegative.setOppositeDirection(Direction.yPositive);
		Direction.zPositive.setOppositeDirection(Direction.zNegative);
		Direction.zNegative.setOppositeDirection(Direction.zPositive);
	}

	int x;
	int y;
	int z;
	String directionLabel;
	Direction oppositeDirection;
	
	Direction(int x, int y, int z, String directionLabel) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.directionLabel = directionLabel;
	}

	public String getDirectionLabel() {
		return directionLabel;
	}
	public void setOppositeDirection(Direction opposite) {
		this.oppositeDirection = opposite;
	}
	public Direction getOppositeDirection() {
		return oppositeDirection;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getZ() {
		return z;
	}
	
	public boolean isDirectionAvailable(int currentX, int currentY, int currentZ, int maxX, int maxY, int maxZ) {
		if (x == 1 && currentX + x >= maxX) {
			return false;
		}
		if (x == -1 && currentX + x < 0) {
			return false;
		}			
		if (y == 1 && currentY + y >= maxY) {
			return false;
		}
		if (y == -1 && currentY + y < 0) {
			return false;
		}			
		if (z == 1 && currentZ + z >= maxZ) {
			return false;
		}
		if (z == -1 && currentZ + z < 0) {
			return false;
		}
		return true;
	}
	
	public boolean isFaceOnOuterBorder(int currentX, int maxX, int currentY, int maxY, int currentZ, int maxZ) {
		return isDirectionAvailable(currentX, maxX, currentY, maxY, currentZ, maxZ);
	}
	
	public static List<Direction> getFacesOnOuterBorder(int currentX, int maxX, int currentY, int maxY, int currentZ, int maxZ) {
		List<Direction> border = new ArrayList<Direction>();
		for (Direction currentDirection : Direction.values()) {
			if (currentDirection.isFaceOnOuterBorder(currentX, maxX, currentY, maxY, currentZ, maxZ)) {
				border.add(currentDirection);
			}
		}
		
		return border;
	}
	
	public Cube findCubeOnFace(Cube cubes[][][], Random cubeOnFaceRandom) {
		int randx;
		int randy;
		int randz;
		switch (this) {
		case xPositive:
			randx = cubes.length - 1;
			randy = cubeOnFaceRandom.nextInt(cubes[randx].length);
			randz = cubeOnFaceRandom.nextInt(cubes[randx][randy].length);
			return cubes[randx][randy][randz];
		case xNegative:
			randx = 0;
			randy = cubeOnFaceRandom.nextInt(cubes[randx].length);
			randz = cubeOnFaceRandom.nextInt(cubes[randx][randy].length);
			return cubes[randx][randy][randz];
		case yPositive:
			randx = cubeOnFaceRandom.nextInt(cubes.length);
			randy = cubes[randx].length - 1;
			randz = cubeOnFaceRandom.nextInt(cubes[randx][randy].length);
			return cubes[randx][randy][randz];
		case yNegative:
			randx = cubeOnFaceRandom.nextInt(cubes.length);
			randy = 0;
			randz = cubeOnFaceRandom.nextInt(cubes[randx][randy].length);
			return cubes[randx][randy][randz];
		case zPositive:
			randx = cubeOnFaceRandom.nextInt(cubes.length);
			randy = cubeOnFaceRandom.nextInt(cubes[randx].length);
			randz = cubes[randx][randy].length - 1;
			return cubes[randx][randy][randz];
		case zNegative:
			randx = cubeOnFaceRandom.nextInt(cubes.length);
			randy = cubeOnFaceRandom.nextInt(cubes[randx].length);
			randz = 0;
			return cubes[randx][randy][randz];
		default: return null;
		}
	}
}