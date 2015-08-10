package org.area515.resinprinter.minercube;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class Cube {
	private TreeMap<Direction, FaceType> faceType = new TreeMap<Direction, FaceType>();
	TreeMap<Direction, Cube> adjacentCubes = new TreeMap<Direction, Cube>();
	int tunnelCount = 0;
	String name;
	Direction solutionDirection = null;

	public Cube(String name) {
		this.name = name;
		closeUnusedFaces();
	}
	
	public void carveTunnelFaces(Direction direction, int travelDistance) {
		Cube endingCube = this;			
		endingCube.faceType.put(direction, FaceType.Open);
		for (int t = 0; t < travelDistance; t++) {
			endingCube.faceType.put(direction, FaceType.Open);
			endingCube = endingCube.adjacentCubes.get(direction);
			endingCube.faceType.put(direction.getOppositeDirection(), FaceType.Open);
		}
	}
	
	public void carveStartingTunnelFace(Direction startingFace) {
		faceType.put(startingFace, FaceType.Open);
	}
	
	public List<Cube> travelToAnExistingTunnel(Direction direction) {
		List<Cube> cubesInTunnel = new ArrayList<Cube>();
		cubesInTunnel.add(this);
		Cube endingCube = adjacentCubes.get(direction);
		while (endingCube != null && endingCube.tunnelCount == 0) {
			cubesInTunnel.add(endingCube);
			endingCube = endingCube.adjacentCubes.get(direction);
		}
		
		//This tunnel is a no go...
		if (endingCube == null) {
			cubesInTunnel.clear();
		}
		
		return cubesInTunnel;
	}
	
	public List<Direction> getAcceptableExitDirections(Direction startingFace, ExitPreference exitPreference) {
		List<Direction> exitDirections = new ArrayList<Direction>(exitPreference.getPossibleExitDirections(startingFace));
		Iterator<Direction> iterator = exitDirections.iterator();
		while (iterator.hasNext()) {
			if (adjacentCubes.get(iterator.next()) != null) {
				iterator.remove();
			}
		}
		
		return exitDirections;
	}
	
	public List<SolutionEntry> getAvailableDirectionsGivenTravelDistances(int minCubes, int maxCubes, Direction dissallowedDirection, boolean allowTunnelsToCollide) {
		List<SolutionEntry> availableDirections = new ArrayList<SolutionEntry>();
		nextDirection : for (Direction currentDirection : adjacentCubes.keySet()) {
			if (dissallowedDirection == currentDirection) {
				continue;
			}
			
			Cube focusCube = this;
			int maxTravel = 0;
			for (maxTravel = 0; maxTravel < maxCubes; maxTravel++) {
				focusCube = focusCube.adjacentCubes.get(currentDirection);
				if (focusCube == null || (!allowTunnelsToCollide && focusCube.tunnelCount > 0)) {
					continue nextDirection;
				}
				
				if (maxTravel+1 >= minCubes) {
					SolutionEntry entry = new SolutionEntry(this);
					entry.direction = currentDirection;
					entry.travelDistance = maxTravel + 1;
					availableDirections.add(entry);
				}
			}
			
		}
		
		return availableDirections;
	}
	
	public void closeUnusedFaces() {
		for (Direction currentDirection : Direction.values()) {
			if (!faceType.containsKey(currentDirection)) {
				faceType.put(currentDirection, FaceType.Closed);
			}
		}
	}
	
	public List<Rectangle> buildZNegative(int myStartingX, int myStartingY, int cubeWidthX, int cubeLengthY, int wallWidthX, int wallLengthY) {
		List<Rectangle> rects = new ArrayList<Rectangle>();
		if (faceType.get(Direction.zNegative) == FaceType.Closed) {
			rects.add(new Rectangle(myStartingX, myStartingY, wallWidthX * 2 + cubeWidthX, wallLengthY * 2 + cubeLengthY));
		}
		
		return rects;
	}
	
	public List<Rectangle> buildZPositive(int myStartingX, int myStartingY, int cubeWidthX, int cubeLengthY, int wallWidthX, int wallLengthY) {
		List<Rectangle> rects = new ArrayList<Rectangle>();
		if (faceType.get(Direction.zPositive) == FaceType.Closed) {
			rects.add(new Rectangle(myStartingX, myStartingY, wallWidthX * 2 + cubeWidthX, wallLengthY * 2 + cubeLengthY));
		}
		
		return rects;
	}
	
	public List<Rectangle> buildXYBorders(int myStartingX, int myStartingY, int cubeWidthX, int cubeLengthY, int wallWidthX, int wallLengthY) {
		List<Rectangle> rects = new ArrayList<Rectangle>();
		for (Map.Entry<Direction, FaceType> currentDirection : faceType.entrySet()) {
			if (currentDirection.getValue() == FaceType.Closed) {
				switch (currentDirection.getKey()) {
				case xPositive : rects.add(new Rectangle(myStartingX + wallWidthX + cubeWidthX, myStartingY, wallWidthX, wallLengthY * 2 + cubeLengthY));continue;
				case xNegative : rects.add(new Rectangle(myStartingX, myStartingY, wallWidthX, wallLengthY * 2 + cubeLengthY));continue;
				case yPositive : rects.add(new Rectangle(myStartingX, myStartingY + wallLengthY + cubeLengthY, wallWidthX * 2 + cubeWidthX, wallLengthY));continue;
				case yNegative : rects.add(new Rectangle(myStartingX, myStartingY, wallWidthX * 2 + cubeWidthX, wallLengthY));continue;
				}
			}
		}
		
		return rects;
	}
	
	public List<Rectangle> buildSupports(int myStartingX, int myStartingY, int cubeWidthX, int cubeLengthY, int wallWidthX, int wallLengthY) {
		List<Rectangle> rects = new ArrayList<Rectangle>();
		
		if (tunnelCount == 0) {
			//outline
			rects.add(new Rectangle(myStartingX + wallWidthX + cubeWidthX, myStartingY, wallWidthX, wallLengthY * 2 + cubeLengthY));
			rects.add(new Rectangle(myStartingX, myStartingY, wallWidthX, wallLengthY * 2 + cubeLengthY));
			rects.add(new Rectangle(myStartingX, myStartingY + wallLengthY + cubeLengthY, wallWidthX * 2 + cubeWidthX, wallLengthY));
			rects.add(new Rectangle(myStartingX, myStartingY, wallWidthX * 2 + cubeWidthX, wallLengthY));
			
			//Supportx
			for (int x = 0; x < cubeWidthX; x += wallWidthX * 2) {
				rects.add(new Rectangle(myStartingX + x, myStartingY, wallWidthX, wallLengthY * 2 + cubeLengthY));
			}//*/
			//Supporty
			for (int y = 0; y < cubeLengthY; y += wallLengthY * 2) {
				rects.add(new Rectangle(myStartingX, myStartingY + y, wallWidthX * 2 + cubeWidthX, wallLengthY));
			}//*/
		}
		
		return rects;
	}

	public String toString() {
		return name;
	}
}