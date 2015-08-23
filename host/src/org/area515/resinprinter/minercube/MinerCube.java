package org.area515.resinprinter.minercube;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.XMLEncoder;
import java.io.File;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.area515.resinprinter.printer.PrinterConfiguration;

@XmlRootElement
public class MinerCube {
	//These parameters don't affect the solution only the size of the cube;
	@XmlElement(name="cubeWallThicknessMillis")
	double cubeWallThicknessMillis = .1;
	@XmlElement(name="innerCubeWidthMillis")
	double innerCubeWidthMillis = 14; //Make this a little larger than your marble.
	
	//These parameters affect the solution and potentially the size of the cube;
	@XmlElement(name="randomSeed")
	long randomSeed = 0;
	@XmlElement(name="cubesPerRow")
	int cubesPerRow = 10;
	@XmlElement(name="exitPreference")
	ExitPreference exitPreference = ExitPreference.OPPOSITE_FACE;
	@XmlElement(name="minimumTurns")
	int minimumTurns = (int)(cubesPerRow * cubesPerRow * cubesPerRow * 0.2d);  		//5% of volume
	@XmlElement(name="maximumTurns")
	int maximumTurns = (int)(cubesPerRow * cubesPerRow * cubesPerRow * 0.6d);    	//7.5% of volume
	@XmlElement(name="allowTunnelsToCollide")
	boolean allowTunnelsToCollide = false;
	@XmlElement(name="deadEndUsageAsAPercentageOfVolumeOfUnusedCubes")
	double deadEndUsageAsAPercentageOfVolumeOfUnusedCubes = 0;						//1 = 100% not 1 cube
	@XmlElement(name="minimumTravelDistanceInCubes")
	int minimumTravelDistanceInCubes = 1;                                        	//1 - 5 with recommended min/max turns ~= 10-15% volume usage
	@XmlElement(name="maximumTravelDistanceInCubes")
	int maximumTravelDistanceInCubes = 5;

	
	//This is the information that changes as a part of the program flow
	long startTime;
	Cube cubes[][][];
	Stack<SolutionEntry> solution = new Stack<SolutionEntry>();
	Cube startingCube;
	List<Cube> untouchedCubes = new ArrayList<Cube>();
	int popOffRate = 0;
	
	//This changes when printing
	Integer currentlyPrintedZSlice = null;
	Integer zNegativeWallThick = null;
	Integer zPositiveWallThick = null;
	Integer zCubeThick = null;
	int zSlicesPerCube;
	int zSlicesPerWall;
	int cubeWidthPixelsX;
	int cubeLengthPixelsY;
	int cubeWallPixelsX;
	int cubeWallPixelsY;
	
	public MinerCube() {
	}
	
	public void buildMaze() {
		startTime = System.currentTimeMillis();
		cubes = new Cube[cubesPerRow][cubesPerRow][cubesPerRow];//X,Y,Z
		byte[] seed = ByteBuffer.allocate(8).putLong(randomSeed).array();
		Random seedGeneratorRandom = new SecureRandom(seed); //Must use a seedGenerator to ensure that successive versions of this program generate exactly the same solutions given the same initial seed and starting parameters.
		seed = ByteBuffer.allocate(8).putLong(seedGeneratorRandom.nextLong()).array();
		Random solutionChoiceRandom = new SecureRandom(seed);
		seed = ByteBuffer.allocate(8).putLong(seedGeneratorRandom.nextLong()).array();
		Random startingCubeRandom = new SecureRandom(seed);
		seed = ByteBuffer.allocate(8).putLong(seedGeneratorRandom.nextLong()).array();
		Random startingfaceRandom = new SecureRandom(seed);
				
		Direction startingFace = Direction.values()[startingfaceRandom.nextInt(Direction.values().length)];

		initializeCubes(cubes);
		startingCube = startingFace.findCubeOnFace(cubes, startingCubeRandom);
		startingCube.tunnelCount = 1;
		solution.push(new SolutionEntry(startingCube));
		
		while (solution.size() < minimumTurns || solution.peek().travelToEndingCube().getAcceptableExitDirections(startingFace, exitPreference).size() == 0) {
			List<SolutionEntry> directionDistancesAvailable = null;
			if (!solution.peek().hasRegisteredSolutions()) {
				Direction lastDirection = null;
				if (solution.size() > 1) {
					lastDirection = solution.get(solution.size() - 2).direction;
	//System.out.println(solution.size() + ". Can't use this direction: " + solution.get(solution.size() - 2));
				}
				directionDistancesAvailable = solution.peek().getStartingCube().getAvailableDirectionsGivenTravelDistances(minimumTravelDistanceInCubes, maximumTravelDistanceInCubes, lastDirection, allowTunnelsToCollide);
		
	//System.out.println(solution.size() + ". getAvailableDirectionsGivenTravelDistances:" + directionDistancesAvailable);
		
				solution.peek().registerSolutions(directionDistancesAvailable);
			}
			if ((directionDistancesAvailable != null && directionDistancesAvailable.size() == 0) || 
				solution.size() > maximumTurns ||
				!solution.peek().areAnySolutionsLeft()) {
				boolean deadEnd = directionDistancesAvailable != null && directionDistancesAvailable.size() == 0;
				boolean reachedMax = solution.size() > maximumTurns;
				boolean noSolutions = !solution.peek().areAnySolutionsLeft();
				SolutionEntry wrongEntry = solution.pop();
				popOffRate++;
	//System.out.println(solution.size() + ". Popped " + wrongEntry + " for these reasons DeadEnd:" + deadEnd + ", ReachedMaxTurns:" + reachedMax + ", AllPoorSolutions:" + noSolutions );
				if (solution.isEmpty()) {
					throw new IllegalArgumentException("Couldn't find a solution to the cube with the parameters that you gave.");
				}
				
				solution.peek().collapseTunnelToEndingCubeSkippingStartingCube();
				solution.peek().addEliminatedSolution();
				continue;
			}
			int random = solutionChoiceRandom.nextInt(solution.peek().possibleSolutions.size());
			SolutionEntry solutionFound = solution.peek().possibleSolutions.get(random);
	//System.out.println(solution.size() + ". Current choices are:" +  solution.peek().possibleSolutions);
			Cube endingCube = solution.peek().tunnelToEndingCubeSkippingStartingCube(solutionFound.direction, solutionFound.travelDistance);
	//System.out.println(solution.size() + ". We randomly chose:" +  solution.peek());
			SolutionEntry entry = new SolutionEntry(endingCube);
			solution.push(entry);
			//System.out.println("Trying:" + entry);
		}

		List<Direction> directionsAvailable = solution.peek().travelToEndingCube().getAcceptableExitDirections(startingFace, exitPreference);
		Direction directionToTravel = directionsAvailable.get(solutionChoiceRandom.nextInt(directionsAvailable.size()));
		solution.peek().direction = directionToTravel;
		
		if (sanityCheck(cubes, allowTunnelsToCollide, null)) {
			throw new IllegalArgumentException("Sanity check failed");
		}
		
		int totalCubes = 0;//cubes.length * cubes[0].length * cubes[0][0].length;
		int cubesTunneled = 0;
		for (int x = 0; x < cubes.length; x++) {
			for (int y = 0; y < cubes[x].length; y++) {
				for (int z = 0; z < cubes[x][y].length; z++) {
					if (cubes[x][y][z].tunnelCount > 0) {
						cubesTunneled++;
					} else {
						untouchedCubes.add(cubes[x][y][z]);
					}
					
					totalCubes++;
				}
			}
		}
		
		
		/*findCube(solution, badTunnelCounts);*/
		
		//Carve out dead ends
		ArrayList<SolutionEntry> deadEnds = new ArrayList<SolutionEntry>();
		int cubesAvailableForDeadEnds = totalCubes - cubesTunneled;
		int needToUseDeadEndCubes = (int)(cubesAvailableForDeadEnds * deadEndUsageAsAPercentageOfVolumeOfUnusedCubes);
		int requiredUnusedCubes = cubesAvailableForDeadEnds - needToUseDeadEndCubes;
		int cubesActuallyUsedInDeadEnds = 0;
		
		nextDeadEnd: while (needToUseDeadEndCubes > 0) {
			Cube currentCube = untouchedCubes.get(solutionChoiceRandom.nextInt(untouchedCubes.size()));
			SolutionEntry entry = new SolutionEntry(currentCube);
			List<Direction> adjacentDirections = new ArrayList<Direction>(currentCube.adjacentCubes.keySet());
			Collections.shuffle(adjacentDirections, solutionChoiceRandom);
			
			Iterator<Direction> directionIter = adjacentDirections.iterator();
		
			while (directionIter.hasNext() && needToUseDeadEndCubes > 0) {
				Direction currentDirection = directionIter.next();
				List<Cube> touchedCubes = currentCube.travelToAnExistingTunnel(currentDirection);

				if (touchedCubes.size() == 0) {
					continue;
				}
				
				for (Cube touchedCube : touchedCubes) {
					untouchedCubes.remove(touchedCube);
				}
				
				needToUseDeadEndCubes -= touchedCubes.size();
				cubesActuallyUsedInDeadEnds += touchedCubes.size();
				entry.tunnelToEndingCubeSkippingEndingCube(currentDirection, touchedCubes.size());
				deadEnds.add(entry);
				continue nextDeadEnd;
			}
			
			//Bad optimization.  This can leave islands of unused cubes causing the untouchedCubes to become improperly low
			//untouchedCubes.remove(currentCube);
		}
		
		if (!deadEnds.isEmpty()) {
			System.out.println("Dead Ends");
			System.out.println("=========");
			for (SolutionEntry currentEntry : deadEnds) {
				System.out.println(currentEntry);// + " arriving at " + currentEntry.travelToEndingCube());
			}
		}
		
		//Fill in closed tunnel faces
		System.out.println("Solution");
		System.out.println("========");
		System.out.println("Starting on the " + startingFace + " face");
		solution.firstElement().getStartingCube().carveStartingTunnelFace(startingFace);
		for (SolutionEntry entry : solution) {
			entry.carveTunnelFaces();
			System.out.println(entry);// + " arriving at " + currentEntry.travelToEndingCube());
		}
		
		//Fill in dead ends
		for (SolutionEntry entry : deadEnds) {
			entry.carveTunnelFaces();
		}
		
		System.out.println("Popoff rate:" + popOffRate);
		System.out.println("Cube generation time:" + (System.currentTimeMillis() - startTime));
		System.out.println("Random Seed:" + randomSeed);
		System.out.println("Minimum number of turns:" + minimumTurns);
		System.out.println("Maximum number of turns:" + maximumTurns);
		System.out.println("Moves to complete solution:" + solution.size());
		System.out.println("Cubes used for solution:" + cubesTunneled);
		System.out.println(String.format("Cubes used for solution %1.1f", ((double)cubesTunneled / (double)(totalCubes)) * 100) + "%");
		System.out.println("Cubes used in dead ends:" + cubesActuallyUsedInDeadEnds);
		System.out.println(String.format("Cubes used in dead ends %1.1f", ((double)cubesActuallyUsedInDeadEnds / (double)(totalCubes)) * 100) + "%");
		System.out.println("Cubes left untouched:" + (totalCubes - cubesTunneled));
		System.out.println(String.format("Cubes left untouched: %1.1f", ((double)(totalCubes - cubesTunneled) / (double)(totalCubes)) * 100) + "%");
		System.out.println("Total cubes:" + totalCubes);


	}

	public static void initializeCubes(Cube cube[][][]) {
		for (int x = 0; x < cube.length; x++) {
			for (int y = 0; y < cube[x].length; y++) {
				for (int z = 0; z < cube[x][y].length; z++) {
					if (cube[x][y][z] == null) {
						cube[x][y][z] = new Cube("x:" + x + ",y:" + y + ",z:" + z);// this should never happen more than once...
					}
					for (Direction currentDirection : Direction.values()) {
						Cube adjacentCube = cube[x][y][z].adjacentCubes.get(currentDirection);
						if (adjacentCube == null && currentDirection.isDirectionAvailable(
								x, y, z, cube.length, cube[x].length, cube[x][y].length)) {
							int adjacentX = x+currentDirection.getX();
							int adjacentY = y+currentDirection.getY();
							int adjacentZ = z+currentDirection.getZ();
							adjacentCube = cube[adjacentX][adjacentY][adjacentZ];
							if (adjacentCube == null) {
								adjacentCube = new Cube("x:" + adjacentX + ",y:" + adjacentY + ",z:" + adjacentZ);
								cube[adjacentX][adjacentY][adjacentZ] = adjacentCube;
							}
							cube[x][y][z].adjacentCubes.put(currentDirection, adjacentCube);
						}
					}
				}
			}
		}
	}
	
	public static boolean sanityCheck(Cube[][][] cube, boolean allowCollisions, Integer knownTouchedCubes) {
		int corners = 0;
		int edges = 0;
		int faces = 0;
		boolean errors = false;
		int actualTouched = 0;
		for (int x = 0; x < cube.length; x++) {
			for (int y = 0; y < cube[x].length; y++) {
				for (int z = 0; z < cube[x][y].length; z++) {
					if (cube[x][y][z].adjacentCubes.size() < 4) {
						//System.out.println("This must be a corner cube:" + cube[x][y][z]);
						corners++;
					}
					if (cube[x][y][z].adjacentCubes.size() < 5) {
						//System.out.println("This must be a corner cube:" + cube[x][y][z]);
						edges++;
					}
					if (cube[x][y][z].adjacentCubes.size() < 6) {
						//System.out.println("This must be a corner cube:" + cube[x][y][z]);
						faces++;
					}
					if (cube[x][y][z].tunnelCount < 0) {
						System.out.println("Bad tunnel count:" + cube[x][y][z] + " of:" + cube[x][y][z].tunnelCount);
						errors = true;
					}
					if (cube[x][y][z].tunnelCount > 1 && !allowCollisions) {
						System.out.println("Bad tunnel count:" + cube[x][y][z] + " of:" + cube[x][y][z].tunnelCount);
						errors = true;
					}
					if (cube[x][y][z].tunnelCount > 0) {
						actualTouched++;
					}
					for (Direction currentDirection : Direction.values()) {
						Cube adjacentCube = cube[x][y][z].adjacentCubes.get(currentDirection);
						int adjacentX = x+currentDirection.getX();
						int adjacentY = y+currentDirection.getY();
						int adjacentZ = z+currentDirection.getZ();
						if (adjacentCube != null && !adjacentCube.name.equals("x:" + adjacentX + ",y:" + adjacentY + ",z:" + adjacentZ)) {
							errors = true;
							System.out.println(cube[x][y][z] + " going direction:" + currentDirection + " is cube:" + adjacentCube + " instead of x:" + adjacentX + ",y:" +adjacentY + ",z:" + adjacentZ);
						}
					}
				}
			}
		}
		if (knownTouchedCubes != null && knownTouchedCubes != actualTouched) {
			System.out.println("knownTouchedCubes:" + knownTouchedCubes + " != actualTouched:" + actualTouched);
			errors = true;
		}
		if (corners != 8) {
			System.out.println("corners:" + corners);
			errors = true;
		}
		//if (edges != ) {
		//  System.out.println("edges:" + edges);
		//	errors = true;
		//}
		//if (faces != 8) {
		//  System.out.println("faces:" + faces);
		//	errors = true;
		//}
		return errors;
	}
	
	public static void findCube(List<SolutionEntry> solution, List<Cube> findCubes) {
		for (SolutionEntry entry : solution) {
			if (findCubes.contains(entry.getStartingCube())) {
				System.out.println("Found starting cube:" + entry);
			}
			for (Cube currentCube : findCubes) {
				if (entry.searchForCubeInTunnel(currentCube)) {
					System.out.println("Found cube in tunnel:" + entry);
				}
			}
		}
	}
	
	public static void printTextCube(Direction startingDirection, Cube startingCube, Cube currentCube, Cube[][][] cubes, List<SolutionEntry> solution) {
		System.out.println("startingCube:" + startingCube);
		System.out.println("startingDirection:" + startingDirection);
		
		for (int z = 0; z < cubes[0][0].length; z++) {
			System.out.println("z:" + z);
			System.out.println("x: 0123456789");
			for (int y = 0; y < cubes[0].length; y++) {
				System.out.print("y:" + y);
				for (int x = 0; x < cubes.length; x++) {
					if (cubes[x][y][z] == currentCube) {
						System.out.print("C");
					} else if (cubes[x][y][z].tunnelCount > 0) {
						System.out.print(cubes[x][y][z].solutionDirection.directionLabel);
					} else {
						System.out.print(" ");
					}
				}
				System.out.println();
			}
			System.out.println();
		}
	}
	
	public boolean hasPrintSlice() {
		return zNegativeWallThick != null || zCubeThick != null || zPositiveWallThick != null;
	}
	
	public Dimension startPrint(double pixelsPerMillimeterX, double pixelsPerMillimeterY, double layerThicknessInMillis) {
		zSlicesPerCube = (int)(innerCubeWidthMillis / layerThicknessInMillis);
		zSlicesPerWall = (int)(cubeWallThicknessMillis / layerThicknessInMillis);
		
		cubeWidthPixelsX = (int)(pixelsPerMillimeterX * innerCubeWidthMillis);
		cubeLengthPixelsY = (int)(pixelsPerMillimeterY * innerCubeWidthMillis);
		cubeWallPixelsX = (int)(pixelsPerMillimeterX * cubeWallThicknessMillis);
		cubeWallPixelsY = (int)(pixelsPerMillimeterY * cubeWallThicknessMillis);
		
		zNegativeWallThick = 0;
		zCubeThick = null;
		zPositiveWallThick = null;
		currentlyPrintedZSlice = 0;
		return new Dimension(
				(cubeWallPixelsX * cubes.length) + cubeWidthPixelsX * cubes.length, 
				(cubeWallPixelsY * cubes[0].length + 1) + cubeLengthPixelsY * cubes.length);
	}
	
	public List<Rectangle> buildNextPrintSlice(int centerX, int centerY) {
		int upperX = centerX - cubes.length * cubeWidthPixelsX / 2;
		int upperY = centerY - cubes[0].length * cubeLengthPixelsY / 2;
		
		List<Rectangle> output = new ArrayList<Rectangle>();
		if (zNegativeWallThick != null) {
			zNegativeWallThick++;
			
			if (zNegativeWallThick > zSlicesPerWall) {
				zCubeThick = 0;
				zNegativeWallThick = null;
			}
		} 
		if (zCubeThick != null) {
			zCubeThick++;
			
			if (zCubeThick > zSlicesPerCube) {
				zCubeThick = 0;
				currentlyPrintedZSlice++;
				
				if (currentlyPrintedZSlice == cubes[0][0].length) {
					zCubeThick = null;
					zPositiveWallThick = 0;
					currentlyPrintedZSlice = cubes[0][0].length - 1;
				}
			}
		} 
		if (zPositiveWallThick != null) {
			zPositiveWallThick++;
			
			if (zPositiveWallThick > zSlicesPerWall) {
				zPositiveWallThick = null;
			}
		}
		
		for (int x = 0; x < cubes.length; x++) {
			for (int y = 0; y < cubes[x].length; y++) {
				if (zNegativeWallThick != null) {
					output.addAll(cubes[x][y][currentlyPrintedZSlice].buildZNegative(upperX + x * (cubeWidthPixelsX + cubeWallPixelsX), upperY + y * (cubeLengthPixelsY + cubeWallPixelsY), cubeWidthPixelsX, cubeLengthPixelsY, cubeWallPixelsX, cubeWallPixelsY));
				} else if (zCubeThick != null) {
					output.addAll(cubes[x][y][currentlyPrintedZSlice].buildXYBorders(upperX + x * (cubeWidthPixelsX + cubeWallPixelsX), upperY + y * (cubeLengthPixelsY + cubeWallPixelsY), cubeWidthPixelsX, cubeLengthPixelsY, cubeWallPixelsX, cubeWallPixelsY));
					output.addAll(cubes[x][y][currentlyPrintedZSlice].buildSupports(upperX + x * (cubeWidthPixelsX + cubeWallPixelsX), upperY + y * (cubeLengthPixelsY + cubeWallPixelsY), cubeWidthPixelsX, cubeLengthPixelsY, cubeWallPixelsX, cubeWallPixelsY));
				} else {//Can't do this!!! if (zPositiveWallThick != null) 
					output.addAll(cubes[x][y][currentlyPrintedZSlice].buildZPositive(upperX + x * (cubeWidthPixelsX + cubeWallPixelsX), upperY + y * (cubeLengthPixelsY + cubeWallPixelsY), cubeWidthPixelsX, cubeLengthPixelsY, cubeWallPixelsX, cubeWallPixelsY));
				}
			}
		}
		
		return output;
	}
}
