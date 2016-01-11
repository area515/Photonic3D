package org.area515.resinprinter.inkdetection.visual;

import org.area515.resinprinter.inkdetection.visual.GenericHoughDetection.HoughReference;

public class Line implements HoughShape {
	private int x1;
	private int x2;
	private int y1;
	private int y2;
	private float theta;
	private int votes;
	private HoughReference houghReference;
	
	public Line(int x1, int y1, int x2, int y2, int votes, HoughReference houghReference) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.votes = votes;
		this.houghReference = houghReference;
	}

	public HoughReference getHoughReference() {
		return houghReference;
	}

	public int[] getMidPoint() {
		return new int[]{(x1 + x2) / 2, (y1 + y2) / 2};
	}
	
	public double getDistanceFromLineMidPointToPoint(int x, int y) {
		int[] midPoint = getMidPoint();
		return Math.sqrt(Math.pow(midPoint[0] - x, 2) + Math.pow(midPoint[1] - y, 2)) * Math.signum(y - midPoint[1]);
	}
	
	public int getX1() {
		return x1;
	}
	public void setX1(int x1) {
		this.x1 = x1;
	}

	public int getX2() {
		return x2;
	}
	public void setX2(int x2) {
		this.x2 = x2;
	}

	public int getY1() {
		return y1;
	}
	public void setY1(int y1) {
		this.y1 = y1;
	}

	public int getY2() {
		return y2;
	}
	public void setY2(int y2) {
		this.y2 = y2;
	}

	public int getVotes() {
		return votes;
	}
	public void setVotes(int votes) {
		this.votes = votes;
	}
	
	public float getTheta() {
		return theta;
	}
	public void setTheta(float theta) {
		this.theta = theta;
	}

	public String toString() {
		return "x1:" + x1 + " y1:" + y1 + " x2:" + x2 + " y2:" + y2 + " votes:" + votes;
	}
}
