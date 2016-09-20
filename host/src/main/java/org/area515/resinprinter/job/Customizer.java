package org.area515.resinprinter.job;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Customizer {
	private String name;
	private String printerName;
	private String printableName;
	private String printableExtension;
	private boolean supportsAffineTransformSettings;        //True means supported/False means not supported
	private AffineTransformSettings affineTransformSettings;//null means it's not used event if this customizer does support the AffineTransform
	private String externalImageAffectingState;
	private SoftReference<BufferedImage> origSliceCache = null;
	private Double zScale = 1.0;
	private String cacheId;
	private int nextSlice = 0;
	private PrinterStep nextStep = PrinterStep.PerformHeader;
	
	public static enum PrinterStep {
		PerformHeader,
		PerformPreSlice,
		PerformExposure,
	}
	
	public static class AffineTransformSettings {
		private Double xTranslate = 0.0;//negative printerwidth for xflip
		private Double yTranslate = 0.0;//negative printerlength for yflip
		private Double xScale = 1.0;//-1 for xflip
		private Double yScale = 1.0;//-1 for yflip		
		private Double xShear = 0.0;
		private Double yShear = 0.0;
		private Double rotation = 0.0;
		private Boolean xFlip = false;
		private Boolean yFlip = false;
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
		
		public Boolean getXFlip() {
			return xFlip;
		}
		public void setXFlip(Boolean xFlip) {
			this.xFlip = xFlip;
		}
		
		public Boolean getYFlip() {
			return yFlip;
		}
		public void setYFlip(Boolean yFlip) {
			this.yFlip = yFlip;
		}
		
		public Double getRotation() {
			return rotation;
		}
		public void setRotation(Double rotation) {
			this.rotation = rotation;
		}

		public Double getXShear() {
			return xShear;
		}
		public void setXShear(Double xShear) {
			this.xShear = xShear;
		}

		public Double getYShear() {
			return yShear;
		}
		public void setYShear(Double yShear) {
			this.yShear = yShear;
		}

		public String getAffineTransformScriptCalculator() {
			return affineTransformScriptCalculator;
		}
		public void setAffineTransformScriptCalculator(String affineTransformScriptCalculator) {
			this.affineTransformScriptCalculator = affineTransformScriptCalculator;
		}

		public AffineTransform createAffineTransform(double width, double height) {
			AffineTransform firstTransform = null;
			if (this.xFlip || this.yFlip) {
				firstTransform = new AffineTransform();
				firstTransform.translate(xFlip?width:0, yFlip?height:0);
				firstTransform.scale(xFlip?-1:1, yFlip?-1:1);
			}
			
			AffineTransform affineTransform = new AffineTransform();
			affineTransform.translate(this.xTranslate, this.yTranslate);
			affineTransform.shear(this.xShear, this.yShear);
			affineTransform.rotate(Math.toRadians(this.rotation), width/2, height/2);
			affineTransform.scale(this.xScale, this.yScale);
			if (firstTransform != null) {
				affineTransform.concatenate(firstTransform);
			}
			return affineTransform;
		} 
		
	}

	public int getNextSlice() {
		return nextSlice;
	}
	public void setNextSlice(int nextSlice) {
		this.nextSlice = nextSlice;
	}

	public PrinterStep getNextStep() {
		return nextStep;
	}
	public void setNextStep(PrinterStep nextStep) {
		this.nextStep = nextStep;
	}

	public AffineTransform createAffineTransform(double width, double height) {
		return this.affineTransformSettings.createAffineTransform(width, height);
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
	
	public Double getZScale() {
		return zScale;
	}
	public void setZScale(Double zScale) {
		this.zScale = zScale;
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

	public String getExternalImageAffectingState() {
		return this.externalImageAffectingState;
	}
	public void setExternalImageAffectingState(String externalImageAffectingState) {
		this.externalImageAffectingState = externalImageAffectingState;
	}
	
	public String getCacheId() {
		if (cacheId != null) {
			return cacheId;
		}
		
		cacheId = "";//This is a trick to stop the original cacheId from being included into the next cacheId computed
		try {
			MessageDigest m = MessageDigest.getInstance("SHA1");
			m.reset();
			ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			mapper.writeValue(output, this);
			m.update(output.toByteArray());
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1, digest);
			cacheId = bigInt.toString(16);
			return cacheId;
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Couldn't serialize Customizer to json.", e);
		}
	}
	public void setCacheId(String cacheId) {
		this.cacheId = null;
	}
	
	@JsonIgnore
	public BufferedImage getOrigSliceCache() {
		if (origSliceCache == null) {
			return null;
		}
		
		return origSliceCache.get();
	}

	@JsonIgnore
	public void setOrigSliceCache(BufferedImage origSliceCache) {
		if (origSliceCache == null) {
			this.origSliceCache = null;
			return;
		}
		
		this.origSliceCache = new SoftReference<BufferedImage>(origSliceCache);
	}
}
