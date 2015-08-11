package org.area515.resinprinter.inkdetection.visual;

public class Circle {
	private int x;
	private int y;
	private int radius;
	private int votes;
	
	public Circle(int x, int y, int radius, int votes) {
		this.x = x;
		this.y = y;
		this.radius = radius;
	}

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}

	public int getRadius() {
		return radius;
	}
	public void setRadius(int radius) {
		this.radius = radius;
	}
	
	public int getVotes() {
		return votes;
	}
	public void setVotes(int votes) {
		this.votes = votes;
	}

	//This code was taken and released from ScriptPhysics (another project of mine)
	public Line intersection(Line line){
        float radius = getRadius();
        int centerX = x;
        int centerY = y;
        
        // A line can intersect a Circle on one point, two points or null.
        float x1 = line.getX1();
        float y1 = line.getY1();
        float x2 = line.getX2();
        float y2 = line.getY2();
        float normalizedX1 = x1 - centerX;
        float normalizedX2 = x2 - centerX;
        float normalizedY1 = y1 - centerY;
        float normalizedY2 = y2 - centerY;
        float distanceX = normalizedX2 - normalizedX1;
        float distanceY = normalizedY2 - normalizedY1;
        float PythSqrd = distanceX * distanceX + distanceY * distanceY;
        float d = normalizedX1 * normalizedY2 - normalizedX2 * normalizedY1;
        float discrim = radius * radius * PythSqrd - d * d;

        // No intersections
        if (discrim <= 0)
            return null;

        float intersectionX1 = (d * distanceY + Math.signum(distanceY) * distanceX * (float)Math.sqrt(discrim)) / PythSqrd + centerX;
        float intersectionY1 = (-d * distanceX + (float)Math.abs(distanceY) * (float)Math.sqrt(discrim)) / PythSqrd + centerY;
        float intersectionX2 = (d * distanceY - Math.signum(distanceY) * distanceX * (float)Math.sqrt(discrim)) / PythSqrd + centerX;
        float intersectionY2 = (-d * distanceX - (float)Math.abs(distanceY) * (float)Math.sqrt(discrim)) / PythSqrd + centerY;

        // Line segment only intersects circle in one place
        if (! isBetween(x1,x2,intersectionX2) || 
            ! isBetween(y1,y2,intersectionY2)) {
            return null;
        }

        // Line segment only intersects circle in one place
        if (! isBetween(x1,x2,intersectionX1) || 
            ! isBetween(y1,y2,intersectionY1)) {
            return null;
        }

        // Line segment intersection circle in two places			
        return new Line((int)intersectionX1, (int)intersectionY1, (int)intersectionX2, (int)intersectionY2, 0);
    }
	
    private final boolean isBetween(float bound1,float bound2, float check){
        if ((bound1 >= check && bound2 <= check) || (bound2 >= check && bound1 <= check))
            return true;
        return false;
    }
    
	public String toString() {
		return "X:" + x + " Y:" + y + " R:" + radius;
	}
}
