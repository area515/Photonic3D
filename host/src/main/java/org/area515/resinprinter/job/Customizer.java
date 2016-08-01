package org.area515.resinprinter.job;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.fasterxml.jackson.annotation.JsonIgnore; 

public class Customizer {
	private String name;
	private String printerName;
	private String printableName;
	private String printableExtension;
	private boolean supportsAffineTransformSettings;        //True means supported/False means not supported
	private AffineTransformSettings affineTransformSettings;//null means it's not used event if this customizer does support the AffineTransform
	private BufferedImage origSliceCache = null;
	
	public static class AffineTransformSettings {
		private Double xTranslate = 0.0;//negative printerwidth for xflip
		private Double yTranslate = 0.0;//negative printerlength for yflip
		private Double xScale = 1.0;//-1 for xflip
		private Double yScale = 1.0;//-1 for yflip
		private String affineTransformScriptCalculator;//Ignore this for now
		
		@JsonIgnore
		public boolean isIdentity() {
			if (xTranslate == 0.0 && yTranslate == 0.0
					&& xScale == 1.0 && yScale == 1.0) {
				return true;
			} else {
				return false;
			}
		}
		
		public Double getXTranslate() {
			return xTranslate;
		}
		public void setXTranslate(Double xTranslate) {
			this.xTranslate = xTranslate;
		}
		
		public Double getYTranslate() {
			//check height bound
			return yTranslate;
		}
		public void setYTranslate(Double yTranslate) {
			this.yTranslate = yTranslate;
		}
		
		public Double getXScale() {
			return xScale;
		}
		public void setXScale(Double xScale) {
			this.xScale = xScale;
		}
		
		public Double getYScale() {
			return yScale;
		}
		public void setYScale(Double yScale) {
			this.yScale = yScale;
		}
		
		public String getAffineTransformScriptCalculator() {
			return affineTransformScriptCalculator;
		}
		public void setAffineTransformScriptCalculator(String affineTransformScriptCalculator) {
			this.affineTransformScriptCalculator = affineTransformScriptCalculator;
		}

		public AffineTransform createAffineTransform() {
			AffineTransform affinetransform = new AffineTransform();
			affinetransform.scale(this.xScale, this.yScale);
			// affinetransform.scale(1, 1);
			// affinetransform.rotate(Math.toRadians(90));
			// affinetransform.translate(0, 0);
			affinetransform.translate(this.xTranslate, this.yTranslate);
			return affinetransform;
		} 
		
	}

	public AffineTransform createAffineTransform() {
		// AffineTransform affinetransform = new AffineTransform();
		// affinetransform.scale(affineTransformSettings.getXScale(), affineTransformSettings.getYScale());
		// affinetransform.translate(affineTransformSettings.getXTranslate, affineTransformSettings.getYTranslate);
		return this.affineTransformSettings.createAffineTransform();
	} 

	public String getPrintableExtension() {
		return printableExtension;
	}

	public void setPrintableExtension(String extension) {
		this.printableExtension = extension;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPrinterName() {
		return printerName;
	}
	public void setPrinterName(String printerName) {
		this.printerName = printerName;
	}
	
	public String getPrintableName() {
		return printableName;
	}
	public void setPrintableName(String printableName) {
		this.printableName = printableName;
	}

	public boolean isSupportsAffineTransformSettings() {
		return supportsAffineTransformSettings;
	}
	public void setSupportsAffineTransformSettings(boolean supportsAffineTransformSettings) {
		this.supportsAffineTransformSettings = supportsAffineTransformSettings;
	}

	public AffineTransformSettings getAffineTransformSettings() {
		return affineTransformSettings;
	}
	public void setAffineTransformSettings(AffineTransformSettings affineTransformSettings) {
		this.affineTransformSettings = affineTransformSettings;
	}

	public BufferedImage getOrigSliceCache() {
		return origSliceCache;
	}

	public void setOrigSliceCache(BufferedImage origSliceCache) {
		this.origSliceCache = origSliceCache;
	}
}
