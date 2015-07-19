package org.area515.resinprinter.inkdetection.visual;

public class CircleDetector implements ShapeDetector<Circle> {
    private int lut[][][]; // LookUp Table for rsin e rcos values
    private int lutAngleCount[];
    private float angleSamplesPerRadius;
    private int radiusMin;
    private int radiusMax;
    private int radiusInc;
    private int radiusCount;  // Hough Space depth (depends on radius interval)
    
    public CircleDetector(float angleSamplesPerRadius, int radiusMin, int radiusMax, int radiusInc) {
    	this.angleSamplesPerRadius = angleSamplesPerRadius;
    	this.radiusMin = radiusMin;
    	this.radiusMax = radiusMax;
    	this.radiusInc = radiusInc;
        this.radiusCount = ((radiusMax-radiusMin)/radiusInc)+1;
    }

    /** The parametric equation for a circle centered at (a,b) with
    radius r is:

	a = x - r*cos(theta)
	b = y - r*sin(theta)
	
	In order to speed calculations, we first construct a lookup
	table (lut) containing the rcos(theta) and rsin(theta) values, for
	theta varying from 0 to 2*PI with increments equal to: 1/pointsOfReference*radiusMax. 
	
	Return value = Number of angles for each radius
	   
	*/
	@Override
	public int[] getHoughSpaceSizeAndGenerateLUT(int imageWidth, int imageHeight) {
	    int incMax = Math.round (angleSamplesPerRadius * radiusMax);  // increment denominator
	
	    lut = new int[2][incMax][radiusCount];
	    lutAngleCount = new int[radiusCount];
	    
	    for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
	        int i = 0;
	        int indexR = (radius-radiusMin)/radiusInc;
	        int incDen = (int)(angleSamplesPerRadius * radius);
	        for(int incNun = 0; incNun < incDen; incNun++) {
	            double angle = (2*Math.PI * (double)incNun) / (double)incDen;
	            int rcos = (int)Math.round ((double)radius * Math.cos (angle));
	            int rsin = (int)Math.round ((double)radius * Math.sin (angle));
	            if((i == 0) | (rcos != lut[0][i][indexR]) & (rsin != lut[1][i][indexR])) {
	                lut[0][i][indexR] = rcos;
	                lut[1][i][indexR] = rsin;
	                i++;
	            }
	        }
	        
	        lutAngleCount[indexR] = i;
	    }
	    
    	return new int[]{imageWidth, imageHeight};
	}

	@Override
	public int getScaleCount() {
		return radiusCount;
	}

	@Override
	public int getMinimumScaleIndex() {
		return radiusMin;
	}

	@Override
	public int getMaximumScaleIndex() {
		return radiusMax;
	}

	@Override
	public int getScaleIncrement() {
		return radiusInc;
	}

	@Override
	public int getSamplesPerScaleIndex(int index) {
		return lutAngleCount[index];
	}

	@Override
	public int[] getSignificantPointOfShape(int x, int y, int sample, int scale) {
        int a = x + lut[1][sample][scale];
        int b = y + lut[0][sample][scale];
        return new int[]{a, b, scale};
	}

	@Override
	public Circle buildShape(int x, int y, int scaleIndex, int votes) {
		return new Circle(x, y, scaleIndex * radiusInc + radiusMin, votes);
	}
}
