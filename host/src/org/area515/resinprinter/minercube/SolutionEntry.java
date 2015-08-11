package org.area515.resinprinter.minercube;
import java.util.List;

public class SolutionEntry {
	private Cube startingCube;
	Direction direction;
	int travelDistance;
	List<SolutionEntry> possibleSolutions;
	private boolean hasSolutions;
	
	public SolutionEntry(Cube startingCube) {
		this.startingCube = startingCube;
	}
	
	public boolean hasRegisteredSolutions() {
		return hasSolutions;
	}
	
	public void registerSolutions(List<SolutionEntry> childSolutions) {
		this.hasSolutions = true;
		this.possibleSolutions = childSolutions;
	}
	
	public boolean addEliminatedSolution() {
		possibleSolutions.remove(this);
		return false;
	}
	
	public boolean areAnySolutionsLeft() {
		return !possibleSolutions.isEmpty();
	}
	
	public Cube travelToEndingCube() {
		Cube endingCube = startingCube;
		for (int t = 0; t < travelDistance; t++) {
			endingCube = endingCube.adjacentCubes.get(direction);
		}
		return endingCube;
	}
	
	public boolean searchForCubeInTunnel(Cube cube) {
		Cube endingCube = startingCube;
		for (int t = 0; t < travelDistance; t++) {
			endingCube = endingCube.adjacentCubes.get(direction);
			if (cube == endingCube) {
				return true;
			}
		}
		return false;
	}
	
	public Cube tunnelToEndingCubeSkippingStartingCube(Direction direction, int travelDistance) {
		this.direction = direction;
		this.travelDistance = travelDistance;
		
		Cube endingCube = startingCube;
		for (int t = 0; t < travelDistance; t++) {
			endingCube.solutionDirection = direction;
			endingCube = endingCube.adjacentCubes.get(direction);
			endingCube.tunnelCount++;
			/*if (endingCube.tunnelCount == 1) {
				cubesTunneled++;
			}*/
		}
		return endingCube;
	}
	
	public Cube tunnelToEndingCubeSkippingEndingCube(Direction direction, int travelDistance) {
		this.direction = direction;
		this.travelDistance = travelDistance;
		
		Cube endingCube = startingCube;
		for (int t = 0; t < travelDistance; t++) {
			endingCube.solutionDirection = direction;
			endingCube.tunnelCount++;
			endingCube = endingCube.adjacentCubes.get(direction);
			/*if (endingCube.tunnelCount == 1) {
				cubesTunneled++;
			}*/
		}
		return endingCube;
	}
	
	public Cube collapseTunnelToEndingCubeSkippingStartingCube() {
		Cube endingCube = startingCube;
		for (int t = 0; t < travelDistance; t++) {
			endingCube = endingCube.adjacentCubes.get(direction);
			endingCube.solutionDirection = null;
			endingCube.tunnelCount--;
			/*if (endingCube.tunnelCount == 0) {
				cubesTunneled--;
			}*/
		}
		return endingCube;
	}
	
	public void carveTunnelFaces() {
		startingCube.carveTunnelFaces(direction, travelDistance);
	}
	
	public Cube getStartingCube() {
		return startingCube;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((direction == null) ? 0 : direction.hashCode());
		result = prime * result
				+ ((startingCube == null) ? 0 : startingCube.hashCode());
		result = prime * result + travelDistance;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SolutionEntry other = (SolutionEntry) obj;
		if (direction != other.direction)
			return false;
		if (startingCube == null) {
			if (other.startingCube != null)
				return false;
		} else if (!startingCube.equals(other.startingCube))
			return false;
		if (travelDistance != other.travelDistance)
			return false;
		return true;
	}
	
	public String toString() {
		return startingCube + " traveling " + travelDistance + " cubes in the " + direction + " direction";
	}
}