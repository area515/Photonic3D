package org.area515.resinprinter.inkdetection.visual;

import org.area515.resinprinter.inkdetection.visual.GenericHoughDetection.HoughReference;

public class LineDetector implements ShapeDetector<Line> {
    private double[] sinCache; 
    private double[] cosCache; 
    private double thetaIncrement;
    private double centerX;
    private double centerY;
    private int thetaCount;
    private int houghHeight;
    private double houghHeightDouble;
    private int imageWidth;
    private int imageHeight;
    private int diagonal;
    
    public LineDetector(double thetaIncrement) {
    	this.thetaIncrement = thetaIncrement;
    }

	@Override
	public int[] getHoughSpaceSizeAndGenerateLUT(int imageWidth, int imageHeight) {
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.diagonal = (int)Math.sqrt(Math.pow((double)imageWidth, 2d) + Math.pow((double)imageHeight, 2d));
		
		thetaCount = (int)(Math.PI / thetaIncrement);
		houghHeightDouble = Math.sqrt(2) * ((double)Math.max(imageHeight, imageWidth)) / 2.0d;
        houghHeight = (int) (Math.sqrt(2) * Math.max(imageHeight, imageWidth)) / 2;
        centerX = imageWidth / 2; 
        centerY = imageHeight / 2; 
        
        sinCache = new double[thetaCount];
        cosCache = new double[thetaCount];
        for (int t = 0; t < thetaCount; t++) { 
            double realTheta = t * thetaIncrement; 
            sinCache[t] = Math.sin(realTheta); 
            cosCache[t] = Math.cos(realTheta); 
        } 
        
        return new int[]{thetaCount, houghHeight * 2};
	}

	@Override
	public int getScaleCount() {
		return 1;
	}

	@Override
	public int getMinimumScaleIndex() {
		return 0;
	}

	@Override
	public int getMaximumScaleIndex() {
		return 0;
	}

	@Override
	public int getScaleIncrement() {
		return 1;
	}

	@Override
	public int getSamplesPerScaleIndex(int index) {
		return thetaCount;
	}

	@Override
	public int getMaximumVotesPerScale(int scaleIndex) {
		return diagonal;
	}
	
	@Override
	public int[] getSignificantPointOfShape(int x, int y, int sample, int scaleIndex) {
        int r = (int) (((x - centerX) * cosCache[sample]) + ((y - centerY) * sinCache[sample]) + houghHeightDouble); 
        if (r < 0 || r >= (houghHeight * 2)) {
        	return null;
        }
        
        return new int[]{sample, r, scaleIndex};
	}

	@Override
	public Line buildShape(int theta, int r, int scaleIndex, int votes) {
        double tsin = sinCache[theta]; 
        double tcos = cosCache[theta]; 
 
        if (theta < Math.PI * 0.25d || theta > Math.PI * 0.75d) { 
            //Vertical lines
            int y1 = 0;
            int x1 = (int) ((((r - houghHeight) - ((y1 - centerY) * tsin)) / tcos) + centerX); 
            int y2 = imageHeight;
            int x2 = (int) ((((r - houghHeight) - ((y2 - centerY) * tsin)) / tcos) + centerX); 
            return new Line(x1, y1, x2, y2, votes, new HoughReference(new int[]{theta, r, scaleIndex}, null));
        } else { 
            //Horizontal lines
            int x1 = 0;
            int y1 = (int) ((((r - houghHeight) - ((x1 - centerX) * tcos)) / tsin) + centerY); 
            int x2 = imageWidth;
            int y2 = (int) ((((r - houghHeight) - ((x2 - centerX) * tcos)) / tsin) + centerY); 
            return new Line(x1, y1, x2, y2, votes, new HoughReference(new int[]{theta, r, scaleIndex}, null));
        } 		
	}
}
