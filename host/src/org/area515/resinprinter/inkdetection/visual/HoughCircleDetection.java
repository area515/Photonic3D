package org.area515.resinprinter.inkdetection.visual;
/** houghCircles_.java:
 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 @author Hemerson Pistori (pistori@ec.ucdb.br) and Eduardo Rocha Costa
 @created 18 de Mar?o de 2004
 
 The Hough Transform implementation was based on 
 Mark A. Schulze applet (http://www.markschulze.net/)
 
*/

//package sigus.templateMatching;
//import sigus.*;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
*   This ImageJ plugin shows the Hough Transform Space and search for
*   circles in a binary image. The image must have been passed through
*   an edge detection module and have edges marked in white (background
*   must be in black).
*   This plugin finds n circles using a basic HoughTransform operator For 
*   better results apply an Edge Detector filter and a binarizer before 
*   using this class
*   
*   Author: Hemerson Pistori (pistori@ec.ucdb.br)"
*/
public class HoughCircleDetection {
    private int radiusMin;  // Find circles with radius grater or equal radiusMin
    private int radiusMax;  // Find circles with radius less or equal radiusMax
    private int radiusInc;  // Increment used to go from radiusMin to radiusMax
    private byte imageValues[]; // Raw image (returned by ip.getPixels())
    private int houghValues[][][]; // Hough Space Values
    private int width; // Hough Space width (depends on image width)
    private int height;  // Hough Space height (depends on image height)
    private int radiusCount;  // Hough Space depth (depends on radius interval)
    private int offset; // Image Width
    private int offx;   // ROI x offset
    private int offy;   // ROI y offset
    private List<Circle> centerPoint; // Center Points of the Circles Found.
    
    
    private int lut[][][]; // LookUp Table for rsin e rcos values
    private int lutAngleCount[];
    
    public  class HoughReference {
    	public int[] reference;
    	
    	public HoughReference(int[] reference) {
    		this.reference = reference;
    	}

    	public String toString() {
    		return "X:" + reference[0] + " Y:" + reference[1] + " R:" + (reference[2] * radiusInc + radiusMin) + " V:" + houghValues[reference[0]][reference[1]][reference[2]];
    	}
    	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(reference);
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
			HoughReference other = (HoughReference) obj;
			if (!Arrays.equals(reference, other.reference))
				return false;
			return true;
		}
    }
    
    private class HoughPrioritizer implements Comparator<HoughReference> {
		@Override
		public int compare(HoughReference o1, HoughReference o2) {
			int val1 = (int)(houghValues[o1.reference[0]][o1.reference[1]][o1.reference[2]]);
			int val2 = (int)(houghValues[o2.reference[0]][o2.reference[1]][o2.reference[2]]);
			int val = val1 - val2;
			int x = o2.reference[0] - o1.reference[0];
			int y = o2.reference[1] - o1.reference[1];
			int r = o2.reference[2] - o1.reference[2];
			if (val != 0) {
				return val;
			}
			if (x != 0) {
				return x;
			}
			if (y != 0) {
				return y;
			}
			return r;
		}
    }
    
    //Threshold is an alternative to maxCircles. All circles with a value in the hough space 
    //greater then threshold are marked. Higher thresholds results in fewer circles.
    public HoughCircleDetection(BufferedImage bufferedImage, Rectangle roi, float angleSamplesPerRadius, int radiusMin, int radiusMax, int radiusInc, int maxCircles, float samplesHitPercentage, boolean usePriorityQueue) {
    	DataBuffer buffer = bufferedImage.getRaster().getDataBuffer();
    	if (!(buffer instanceof DataBufferByte)) {
    		throw new IllegalArgumentException("The color depth of the bufferedImage must be 8bit gray");
    	}
    	
        imageValues =  ((DataBufferByte)buffer).getData();
        if (roi == null) {
        	roi = new Rectangle(bufferedImage.getWidth(), bufferedImage.getHeight());
        }


        offx = roi.x;
        offy = roi.y;
        width = roi.width;
        height = roi.height;
        offset = bufferedImage.getWidth();//ip.getWidth();

        this.radiusMin = radiusMin;
        this.radiusMax = radiusMax;
        this.radiusInc = radiusInc;
        radiusCount = ((radiusMax-radiusMin)/radiusInc)+1;
        
        boolean useThreshold = false;
        if (maxCircles > 0) {
            useThreshold = false;
            samplesHitPercentage = -1;
        } else {
            useThreshold = true;
            if(samplesHitPercentage < 0) {
                throw new IllegalArgumentException("Threshold must be greater than 0 when maxCircles is 0");
            }
        }
        
        FixedSizePriorityQueue<HoughReference> mostLikelyCircles = usePriorityQueue?new FixedSizePriorityQueue<>(maxCircles, new HoughPrioritizer()):null;
        houghTransform(angleSamplesPerRadius, mostLikelyCircles);
        
        // Mark the center of the found circles in a new image
        if (mostLikelyCircles != null) {
            buildCenterPointsByPriority(mostLikelyCircles);
        } else if(useThreshold) {
            buildCenterPointsByThreshold(samplesHitPercentage);
        } else {
            buildCenterPoints(maxCircles);
        }
    }

    /** The parametric equation for a circle centered at (a,b) with
        radius r is:

    a = x - r*cos(theta)
    b = y - r*sin(theta)

    In order to speed calculations, we first construct a lookup
    table (lut) containing the rcos(theta) and rsin(theta) values, for
    theta varying from 0 to 2*PI with increments equal to: 1/steps*radiusMax. 

    Return value = Number of angles for each radius
       
    */
    private void buildLookUpTable(float steps) {
        int incMax = Math.round (steps * radiusMax);  // increment denominator

        lut = new int[2][incMax][radiusCount];
        lutAngleCount = new int[radiusCount];
        
        for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
            int i = 0;
            int indexR = (radius-radiusMin)/radiusInc;
            int incDen = (int)(steps * radius);
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
    }

    private void houghTransform (float steps, FixedSizePriorityQueue<HoughReference> houghPriorityQueue) {

        buildLookUpTable(steps);

        houghValues = new int[width][height][radiusCount];
        
        int k = width - 1;
        int l = height - 1;
        
        for(int y = 1; y < l; y++) {
            for(int x = 1; x < k; x++) {
                for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
                    if( imageValues[(x+offx)+(y+offy)*offset] != 0 )  {// Edge pixel found
                        int indexR=(radius-radiusMin)/radiusInc;
                        for(int i = 0; i < lutAngleCount[indexR]; i++) {

                            int a = x + lut[1][i][indexR]; 
                            int b = y + lut[0][i][indexR]; 
                            if((b >= 0) & (b < height) & (a >= 0) & (a < width)) {
                                houghValues[a][b][indexR] += 1;
                                if (houghPriorityQueue != null) {
                                	houghPriorityQueue.add(new HoughReference(new int[]{a, b, indexR}));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Search for a fixed number of circles.

    @param maxCircles The number of circles that should be found.  
    */
    private void buildCenterPoints (int maxCircles) {
        centerPoint = new ArrayList<Circle>();
        int xMax = 0;
        int yMax = 0;
        int rMax = 0;
        
        for(int c = 0; c < maxCircles; c++) {
            int counterMax = -1;
            for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
            	
                int indexR = (radius-radiusMin)/radiusInc;
                for(int y = 0; y < height; y++) {
                    for(int x = 0; x < width; x++) {
                        if(houghValues[x][y][indexR] > counterMax) {
                            counterMax = houghValues[x][y][indexR];
                            xMax = x;
                            yMax = y;
                            rMax = radius;
                        }
                    }

                }
            }
            
            if (counterMax > 0) {
            	centerPoint.add(new Circle (xMax, yMax, rMax, counterMax));
            	clearNeighbours(xMax,yMax,rMax);
            }
        }
    }

    
    private void buildCenterPointsByPriority(FixedSizePriorityQueue<HoughReference> mostLikelyCircles) {
        centerPoint = new ArrayList<Circle>();
        
        for (HoughReference potentialCircle : mostLikelyCircles) {
        	centerPoint.add(new Circle(potentialCircle.reference[0], potentialCircle.reference[1], potentialCircle.reference[2] * radiusInc + radiusMin, houghValues[potentialCircle.reference[0]][potentialCircle.reference[1]][potentialCircle.reference[2]]));
        }
    }


    /** 
     * Search circles having values in the hough space higher than a threshold
     * @param threshold The threshold used to select the higher point of Hough Space
     */
    private void buildCenterPointsByThreshold (float samplesHitPercentage) {

        centerPoint = new ArrayList<Circle>();
        int xMax = 0;
        int yMax = 0;

        for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
            int indexR = (radius-radiusMin)/radiusInc;
            for(int y = 0; y < height; y++) {
                for(int x = 0; x < width; x++) {
                    if(houghValues[x][y][indexR] > (samplesHitPercentage * lutAngleCount[indexR])) {
                        centerPoint.add(new Circle(x, y, radius, houghValues[x][y][indexR]));

                        clearNeighbours(xMax,yMax,radius);
                    }
                }
            }
        }
    }

    /** Clear, from the Hough Space, all the counter that are near (radius/2) a previously found circle C.
        
    @param x The x coordinate of the circle C found.
    @param x The y coordinate of the circle C found.
    @param x The radius of the circle C found.
    */
    private void clearNeighbours(int x,int y, int radius) {
        // The following code just clean the points around the center of the circle found.
        double halfRadius = radius / 2.0F;
        double halfSquared = halfRadius*halfRadius;

        int y1 = (int)Math.floor ((double)y - halfRadius);
        int y2 = (int)Math.ceil ((double)y + halfRadius) + 1;
        int x1 = (int)Math.floor ((double)x - halfRadius);
        int x2 = (int)Math.ceil ((double)x + halfRadius) + 1;

        if(y1 < 0)
            y1 = 0;
        if(y2 > height)
            y2 = height;
        if(x1 < 0)
            x1 = 0;
        if(x2 > width)
            x2 = width;

        for(int r = radiusMin;r <= radiusMax;r = r+radiusInc) {
            int indexR = (r-radiusMin)/radiusInc;
            for(int i = y1; i < y2; i++) {
                for(int j = x1; j < x2; j++) {	      	     
                    if(Math.pow (j - x, 2D) + Math.pow (i - y, 2D) < halfSquared) {
                        houghValues[j][i][indexR] = 0;
                    }
                }
            }
        }
    }
    
   /*private boolean outOfBounds(int y,int x) {
        if(x >= width)
            return(true);
        if(x <= 0)
            return(true);
        if(y >= height)
            return(true);
        if(y <= 0)
            return(true);
        return(false);
    }*/
     
    public List<Circle> getCircles() {
    	return centerPoint;
    }
    
    public void printHoughSpace() {
        for(int radius = radiusMin;radius <= radiusMax;radius = radius+radiusInc) {
        	int indexR=(radius-radiusMin)/radiusInc;
        	System.out.println(printHoughSpaceForRadius(indexR));
        }
    }
    
    private String printHoughSpaceForRadius(int radiusIndex) {
    	StringBuilder builder = new StringBuilder();
    	int radius = radiusIndex * radiusInc + radiusMin;
    	builder.append("Radius:" + radius + "\n   ");
		for (int x = 0; x < width; x++) {
			builder.append(String.format("%1$2d.", x));
		}
		builder.append("\n");
    	for (int y = 0; y < height; y++) {
			builder.append(String.format("%1$2d:", y));
    		for (int x = 0; x < width; x++) {
    			builder.append(String.format("%1$02.0f-", houghValues[x][y][radiusIndex]));
    		}
			builder.append("\n");
    	}
    	return builder.toString();
    }
}