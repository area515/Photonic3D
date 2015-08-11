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
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
*   This class is the Hough Transform Space and search for
*   generic objects in a binary image. The image must have been passed through
*   an edge detection module and have edges marked in white (background
*   must be in black).
*   This code is loosely based off of Hemerson Pistori (pistori@ec.ucdb.br)
*   Hough Circle Detection code. I made it generic to allow the detection of 
*   arbitrary shapes.
*/
public class GenericHoughDetection<S> {
    private byte imageValues[]; // Raw image (returned by ip.getPixels())
    private int houghValues[][][]; // Hough Space Values
    private int width; // Hough Space width (depends on image width)
    private int height;  // Hough Space height (depends on image height)
    private int offset; // Image Width
    private int offx;   // ROI x offset
    private int offy;   // ROI y offset
    private List<S> centerPoint; // Center Points of the Circles Found.
    private FixedSizePriorityQueue<HoughReference> mostLikelyShape;
    private ShapeDetector<S> detector;
    private boolean useThreshold = false;
    private int maxShapes;
    private float samplesHitPercentage;
    private int scaleCount;
    private int scaleMin;
    private int scaleMax;
    private int scaleInc;
    private int[] houghSpaceSize;
    
    public  class HoughReference {
    	public int[] reference;
    	
    	public HoughReference(int[] reference) {
    		this.reference = reference;
    	}

    	public String toString() {
    		return "X:" + reference[0] + " Y:" + reference[1] + " R:" + (reference[2] * scaleInc + scaleMin) + " V:" + houghValues[reference[0]][reference[1]][reference[2]];
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
    
    /**
     * @param bufferedImage
     * @param roi
     * @param detector
     * @param samplesHitPercentage the percentage of theta samples that must be hit before we consider it a full shape.
     * @param maxShapes
     * @param usePriorityQueue
     */
    public GenericHoughDetection(BufferedImage bufferedImage, Rectangle roi, ShapeDetector<S> detector, float samplesHitPercentage, int maxShapes, boolean usePriorityQueue) {
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
        houghSpaceSize = detector.getHoughSpaceSizeAndGenerateLUT(width, height);
        offset = bufferedImage.getWidth();
        this.detector = detector;
        scaleCount = detector.getScaleCount();
        scaleMin = detector.getMinimumScaleIndex();
        scaleMax = detector.getMaximumScaleIndex();
        scaleInc = detector.getScaleIncrement();
        this.maxShapes = maxShapes;
        this.samplesHitPercentage = samplesHitPercentage;
        
        if (maxShapes > 0) {
            useThreshold = false;
            samplesHitPercentage = -1;
        } else {
            useThreshold = true;
            if(samplesHitPercentage < 0) {
                throw new IllegalArgumentException("Threshold must be greater than 0 when maxCircles is 0");
            }
        }
        
        mostLikelyShape = usePriorityQueue?new FixedSizePriorityQueue<>(maxShapes, new HoughPrioritizer()):null;
    }
    
    public void houghTransform() {
        houghValues = new int[houghSpaceSize[0]][houghSpaceSize[1]][scaleCount];
        
        for(int y = 1; y < height; y++) {
            for(int x = 1; x < width; x++) {
                for(int scale = scaleMin;scale <= scaleMax;scale = scale+scaleInc) {
                    if( imageValues[(x+offx)+(y+offy)*offset] != 0 )  {// Edge pixel found
                        int scaleIndex=(scale-scaleMin)/scaleInc;
                        int samples = detector.getSamplesPerScaleIndex(scaleIndex);
                        for(int sample = 0; sample < samples; sample++) {
                        	int[] xyScale = detector.getSignificantPointOfShape(x, y, sample, scaleIndex);
                        	
                            if(xyScale != null && ((xyScale[1] >= 0) & (xyScale[1] < houghSpaceSize[1]) & (xyScale[0] >= 0) & (xyScale[0] < houghSpaceSize[0]))) {
                                houghValues[xyScale[0]][xyScale[1]][xyScale[2]] += 1;
                                if (mostLikelyShape != null) {
                                	mostLikelyShape.add(new HoughReference(new int[]{xyScale[0], xyScale[1], xyScale[2]}));
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Mark the center of the found circles in a new image
        if (mostLikelyShape != null) {
            buildCenterPointsByPriority(mostLikelyShape);
        } else if(useThreshold) {
            buildCenterPointsByThreshold(samplesHitPercentage);
        } else {
            buildCenterPoints(maxShapes);
        }
    }
    
    /** Search for a fixed number of circles.

    @param maxShapes The number of circles that should be found.  
    */
    private void buildCenterPoints (int maxShapes) {
        centerPoint = new ArrayList<S>();
        int xMax = 0;
        int yMax = 0;
        int currentScaleMax = 0;
        
        for(int c = 0; c < maxShapes; c++) {
            int houghVotes = -1;
            for(int scale = scaleMin;scale <= scaleMax;scale = scale+scaleInc) {
                int scaleIndex = (scale-scaleMin)/scaleInc;
                for(int y = 0; y < houghSpaceSize[1]; y++) {
                    for(int x = 0; x < houghSpaceSize[0]; x++) {
                        if(houghValues[x][y][scaleIndex] > houghVotes) {
                            houghVotes = houghValues[x][y][scaleIndex];
                            xMax = x;
                            yMax = y;
                            currentScaleMax = scale;
                        }
                    }
                }
            }
            
            if (houghVotes > 0) {
            	centerPoint.add(detector.buildShape(xMax, yMax, currentScaleMax, houghVotes));
            	clearNeighbours(xMax,yMax,currentScaleMax);
            }
        }
    }

    
    private void buildCenterPointsByPriority(FixedSizePriorityQueue<HoughReference> mostLikelyCircles) {
        centerPoint = new ArrayList<S>();
        
        for (HoughReference potentialCircle : mostLikelyCircles) {
        	S shape = detector.buildShape(potentialCircle.reference[0], potentialCircle.reference[1], potentialCircle.reference[2], houghValues[potentialCircle.reference[0]][potentialCircle.reference[1]][potentialCircle.reference[2]]);
        	centerPoint.add(shape);
        }
    }


    /** 
     * Search circles having values in the hough space higher than a threshold
     * @param threshold The threshold used to select the higher point of Hough Space
     */
    private void buildCenterPointsByThreshold (float samplesHitPercentage) {
        centerPoint = new ArrayList<S>();
        int xMax = 0;
        int yMax = 0;

        for(int scale = scaleMin;scale <= scaleMax;scale = scale+scaleInc) {
            int scaleIndex = (scale-scaleMin)/scaleInc;
            int samples = detector.getSamplesPerScaleIndex(scaleIndex);
            for(int y = 0; y < houghSpaceSize[1]; y++) {
                for(int x = 0; x < houghSpaceSize[0]; x++) {
                    if(houghValues[x][y][scaleIndex] > (samplesHitPercentage * samples)) {
                        centerPoint.add(detector.buildShape(x, y, scaleIndex, houghValues[x][y][scaleIndex]));
                        clearNeighbours(xMax,yMax,scale);
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
        // The following code just clean the points around the center of the image found.
        double halfRadius = radius / 2.0F;
        double halfSquared = halfRadius*halfRadius;

        int y1 = (int)Math.floor ((double)y - halfRadius);
        int y2 = (int)Math.ceil ((double)y + halfRadius) + 1;
        int x1 = (int)Math.floor ((double)x - halfRadius);
        int x2 = (int)Math.ceil ((double)x + halfRadius) + 1;

        if(y1 < 0)
            y1 = 0;
        if(y2 > houghSpaceSize[1])
            y2 = houghSpaceSize[1];
        if(x1 < 0)
            x1 = 0;
        if(x2 > houghSpaceSize[0])
            x2 = houghSpaceSize[0];

        for(int currentScale = scaleMin;currentScale <= scaleMax;currentScale = currentScale+scaleInc) {
            int scaleIndex = (currentScale-scaleMin)/scaleInc;
            for(int i = y1; i < y2; i++) {
                for(int j = x1; j < x2; j++) {
                    if(Math.pow (j - x, 2D) + Math.pow (i - y, 2D) < halfSquared) {
                        houghValues[j][i][scaleIndex] = 0;
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
     
    public List<S> getShapes() {
    	return centerPoint;
    }
    
    public void printHoughSpace() {
        for(int currentScale = scaleMin;currentScale <= scaleMax;currentScale = currentScale+scaleInc) {
        	int scaleIndex=(currentScale-scaleMin)/scaleInc;
        	System.out.println(printHoughSpaceForScale(scaleIndex));
        }
    }
    
    private String printHoughSpaceForScale(int scaleIndex) {
    	StringBuilder builder = new StringBuilder();
    	double radius = scaleIndex * scaleInc + scaleMin;
    	builder.append("Radius:" + radius + "\n   ");
		for (int x = 0; x < houghSpaceSize[0]; x++) {
			builder.append(String.format("%1$2d.", x));
		}
		builder.append("\n");
    	for (int y = 0; y < houghSpaceSize[1]; y++) {
			builder.append(String.format("%1$2d:", y));
    		for (int x = 0; x < houghSpaceSize[0]; x++) {
    			builder.append(String.format("%1$02.0f-", houghValues[x][y][scaleIndex]));
    		}
			builder.append("\n");
    	}
    	return builder.toString();
    }
    
    public BufferedImage generateHoughSpaceImage(boolean performLinear8bitEqualizationPass) {
    	//TODO: For performance reasons we really need to use the databuffer rather than this get/set pixel stuff
    	BufferedImage image = new BufferedImage(houghSpaceSize[0], houghSpaceSize[1], BufferedImage.TYPE_BYTE_GRAY);
    	WritableRaster d = image.getRaster();
    	double highestMax = 0;
    	int sumarizedHoughValues[][] = new int[houghSpaceSize[1]][houghSpaceSize[0]];
    	for (int y = 0; y < houghSpaceSize[1]; y++) {
    		for (int x = 0; x < houghSpaceSize[0]; x++) {
    			for (int scale = scaleMin; scale <= scaleMax; scale += scaleInc) {
    				int scaleIndex = (scale-scaleMin)/scaleInc;
    				sumarizedHoughValues[x][y] += houghValues[x][y][scaleIndex];
    			}
    			if (sumarizedHoughValues[x][y] > highestMax) {
    				highestMax = sumarizedHoughValues[x][y];
    			}

				d.setPixel(x, y, new int[]{sumarizedHoughValues[x][y]});
    		}
    	}
    	
    	if (performLinear8bitEqualizationPass) {
        	for (int y = 0; y < houghSpaceSize[1]; y++) {
        		for (int x = 0; x < houghSpaceSize[0]; x++) {
        			int value[] = d.getPixel(x, y, new int[1]);
        			value[0] = (int)((double)sumarizedHoughValues[x][y] / highestMax * 255d);
    				d.setPixel(x, y, value);
        		}
        	}    	
        }
    	
    	return image;
    }
    
    public BufferedImage generateHoughSpaceImageForScale(int scaleIndex) {
    	BufferedImage image = new BufferedImage(houghSpaceSize[0], houghSpaceSize[1], BufferedImage.TYPE_BYTE_GRAY);
    	WritableRaster d = image.getRaster();
    	for (int y = 0; y < houghSpaceSize[1]; y++) {
    		for (int x = 0; x < houghSpaceSize[0]; x++) {
    			d.setPixel(x, y, new int[]{houghValues[x][y][scaleIndex]});
    		}
    	}
    	
    	return image;
    }
}