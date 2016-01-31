package org.area515.resinprinter.minercube;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@XmlRootElement
public class MinerCube {
	private static final Logger logger = LogManager.getLogger();
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
					logger.debug("{}. Can't use this direction: {}",solution.size(), solution.get(solution.size() - 2));
				}
				directionDistancesAvailable = solution.peek().getStartingCube().getAvailableDirectionsGivenTravelDistances(minimumTravelDistanceInCubes, maximumTravelDistanceInCubes, lastDirection, allowTunnelsToCollide);
		
				logger.debug("{}. getAvailableDirectionsGivenTravelDistances:{}", solution.size(), directionDistancesAvailable);
		
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
				logger.debug("{}. Popped {} for these reasons DeadEnd:{}, ReachedMaxTurns:{}, AllPoorSolutions:{}", solution.size(), wrongEntry, deadEnd, reachedMax, noSolutions);
				if (solution.isEmpty()) {
					throw new IllegalArgumentException("Couldn't find a solution to the cube with the parameters that you gave.");
				}
				
				solution.peek().collapseTunnelToEndingCubeSkippingStartingCube();
				solution.peek().addEliminatedSolution();
				continue;
			}
			int random = solutionChoiceRandom.nextInt(solution.peek().possibleSolutions.size());
			SolutionEntry solutionFound = solution.peek().possibleSolutions.get(random);
			logger.debug("{}. Current choices are:{}", solution.size(), solution.peek().possibleSolutions);
			Cube endingCube = solution.peek().tunnelToEndingCubeSkippingStartingCube(solutionFound.direction, solutionFound.travelDistance);
			logger.debug("{}. We randomly chose:{}",solution.size(), solution.peek());
			SolutionEntry entry = new SolutionEntry(endingCube);
			solution.push(entry);
			logger.debug("Trying:{}", entry);
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
			logger.info("Dead Ends");
			logger.info("=========");
			for (SolutionEntry currentEntry : deadEnds) {
				logger.info(currentEntry);// + " arriving at " + currentEntry.travelToEndingCube());
			}
		}
		
		//Fill in closed tunnel faces
		logger.info("Solution");
		logger.info("========");
		logger.info("Starting on the {} face", startingFace);
		solution.firstElement().getStartingCube().carveStartingTunnelFace(startingFace);
		for (SolutionEntry entry : solution) {
			entry.carveTunnelFaces();
			logger.info(entry);// + " arriving at " + currentEntry.travelToEndingCube());
		}
		
		//Fill in dead ends
		for (SolutionEntry entry : deadEnds) {
			entry.carveTunnelFaces();
		}
		
		logger.info("Popoff rate:{}", popOffRate);
		logger.info("Cube generation time:{}", System.currentTimeMillis() - startTime);
		logger.info("Random Seed:{}", randomSeed);
		logger.info("Minimum number of turns:{}", minimumTurns);
		logger.info("Maximum number of turns:{}", maximumTurns);
		logger.info("Moves to complete solution:{}", solution.size());
		logger.info("Cubes used for solution:{}", cubesTunneled);
		logger.info("Cubes used for solution %1.1f%%", ((double)cubesTunneled / (double)(totalCubes)) * 100);
		logger.info("Cubes used in dead ends:{}", cubesActuallyUsedInDeadEnds);
		logger.info("Cubes used in dead ends %1.1f%%", ((double)cubesActuallyUsedInDeadEnds / (double)(totalCubes)) * 100);
		logger.info("Cubes left untouched:{}", (totalCubes - cubesTunneled));
		logger.info("Cubes left untouched: %1.1f%%", ((double)(totalCubes - cubesTunneled) / (double)(totalCubes)) * 100);
		logger.info("Total cubes:{}", totalCubes);


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
						logger.debug("This must be a corner cube:{}", cube[x][y][z]);
						corners++;
					}
					if (cube[x][y][z].adjacentCubes.size() < 5) {
						logger.debug("This must be a corner cube:{}", cube[x][y][z]);
						edges++;
					}
					if (cube[x][y][z].adjacentCubes.size() < 6) {
						logger.debug("This must be a corner cube:{}", cube[x][y][z]);
						faces++;
					}
					if (cube[x][y][z].tunnelCount < 0) {
						logger.info("Bad tunnel count:{} of:{}", cube[x][y][z], cube[x][y][z].tunnelCount);
						errors = true;
					}
					if (cube[x][y][z].tunnelCount > 1 && !allowCollisions) {
						logger.info("Bad tunnel count:{} of:{}", cube[x][y][z], cube[x][y][z].tunnelCount);
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
							logger.info("{} going direction:{} is cube:{} instead of x:{},y:{},z:{}", cube[x][y][z], currentDirection, adjacentCube, adjacentX, adjacentY, adjacentZ);
						}
					}
				}
			}
		}
		if (knownTouchedCubes != null && knownTouchedCubes != actualTouched) {
			logger.info("knownTouchedCubes:{} != actualTouched:{}", knownTouchedCubes, actualTouched);
			errors = true;
		}
		if (corners != 8) {
			logger.info("corners:{}", corners);
			errors = true;
		}
		return errors;
	}
	
	public static void findCube(List<SolutionEntry> solution, List<Cube> findCubes) {
		for (SolutionEntry entry : solution) {
			if (findCubes.contains(entry.getStartingCube())) {
				logger.info("Found starting cube:{}", entry);
			}
			for (Cube currentCube : findCubes) {
				if (entry.searchForCubeInTunnel(currentCube)) {
					logger.info("Found cube in tunnel:{}", entry);
				}
			}
		}
	}
	
	public static void printTextCube(Direction startingDirection, Cube startingCube, Cube currentCube, Cube[][][] cubes, List<SolutionEntry> solution) {
		logger.info("startingCube:{}", startingCube);
		logger.info("startingDirection:{}", startingDirection);
		
		StringBuilder builder = new StringBuilder();
		for (int z = 0; z < cubes[0][0].length; z++) {
			logger.info("z:{}", z);
			logger.info("x: 0123456789");
			for (int y = 0; y < cubes[0].length; y++) {
				builder.append("y:");
				builder.append(y);
				for (int x = 0; x < cubes.length; x++) {
					if (cubes[x][y][z] == currentCube) {
						builder.append("C");
					} else if (cubes[x][y][z].tunnelCount > 0) {
						builder.append(cubes[x][y][z].solutionDirection.directionLabel);
					} else {
						builder.append(" ");
					}
				}
				logger.info(builder);
				builder.delete(0, builder.length());
			}
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
