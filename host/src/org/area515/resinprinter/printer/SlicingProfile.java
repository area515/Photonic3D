package org.area515.resinprinter.printer;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.area515.resinprinter.inkdetection.PrintMaterialDetector;
import org.area515.resinprinter.job.InkDetector;
import org.area515.util.TemplateEngine;

import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlRootElement(name="SliceBuildConfig")
public class SlicingProfile {
	public static class InkConfig {
		@XmlElement(name="PrintMaterialDetector")
		private String printMaterialDetector;
		@XmlElement(name="PercentageOfPrintMaterialConsideredEmpty")
		private float percentageConsideredEmpty;
		@XmlElement(name="Name")
	    private String name;
	    @XmlElement(name="SliceHeight")
	    private double sliceHeight;
	    @XmlElement(name="LayerTime")
	    private int layerTime;
	    @XmlElement(name="FirstLayerTime")
	    private int firstLayerTime;
	    @XmlElement(name="NumberofBottomLayers")
	    private int numberOfFirstLayers;
	    @XmlElement(name="ResinPriceL")
	    private double resinPriceL;
		private InkDetector detector;
		
		@XmlTransient
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		@XmlTransient
		public double getSliceHeight() {
			return sliceHeight;
		}
		public void setSliceHeight(double sliceHeight) {
			this.sliceHeight = sliceHeight;
		}
		
		@XmlTransient
		public int getNumberOfFirstLayers() {
			return numberOfFirstLayers;
		}
		public void setNumberOfFirstLayers(int numberOfFirstLayers) {
			this.numberOfFirstLayers = numberOfFirstLayers;
		}
		
		@XmlTransient
		public double getResinPriceL() {
			return resinPriceL;
		}
		public void setResinPriceL(double resinPriceL) {
			this.resinPriceL = resinPriceL;
		}
		
		@XmlTransient
		public int getExposureTime() {
			return layerTime;
		}
		public void setExposureTime(int layerTime) {
			this.layerTime = layerTime;
		}
		
		@XmlTransient
		public int getFirstLayerExposureTime() {
			return firstLayerTime;
		}
		public void setFirstLayerExposureTime(int firstLayerTime) {
			this.firstLayerTime = firstLayerTime;
		}
		
		@XmlTransient
		public String getPrintMaterialDetector() {
			return printMaterialDetector;
		}
		public void setPrintMaterialDetector(String printMaterialDetector) {
			this.printMaterialDetector = printMaterialDetector;
		}
		
		@XmlTransient
		public float getPercentageOfInkConsideredEmpty() {
			return percentageConsideredEmpty;
		}
		public void setPercentageOfInkConsideredEmpty(float percentageConsideredEmpty) {
			this.percentageConsideredEmpty = percentageConsideredEmpty;
		}
		
		public InkDetector getInkDetector(Printer printer) {
			if (this.detector != null) {
				return this.detector;
			}
			
			String detectorClass = getPrintMaterialDetector();
			if (detectorClass == null || detectorClass.trim().isEmpty()) {
				return null;
			}
			
			try {
				this.detector = new InkDetector(printer, ((Class<PrintMaterialDetector>)Class.forName(detectorClass)).newInstance(), percentageConsideredEmpty);
				return this.detector;
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				System.out.println("Failed to load PrintMaterialDetector:" + detector);
				return null;
			}
		}
	}

    @XmlElement(name="DotsPermmX")
	private double dotsPermmX;
    @XmlElement(name="DotsPermmY")
	private double dotsPermmY;
    @XmlElement(name="XResolution")
	private int xResolution;
    @XmlElement(name="YResolution")
	private int yResolution;
    @XmlElement(name="BlankTime")
	private int blankTime;
    @XmlElement(name="PlatformTemp")
	private int platformTemp;
    @XmlElement(name="ExportSVG")
	private int exportSVG;
    @XmlElement(name="Export")
	private boolean export;
    @XmlElement(name="ExportPNG")
	private boolean exportPNG;
    @XmlElement(name="XOffset")
	private Integer xOffset;
    @XmlElement(name="YOffset")
	private Integer yOffset;
    @XmlElement(name="Direction")
	private BuildDirection direction;
    @XmlElement(name="LiftDistance")
	private double liftDistance;
    @XmlElement(name="SlideTiltValue")
	private int slideTiltValue;
    @XmlElement(name="AntiAliasing")
	private boolean antiAliasing;
    @XmlElement(name="UseMainLiftGCode")
	private boolean useMainLiftGCode;
    @XmlElement(name="AntiAliasingValue")
	private double antiAliasingValue;
    @XmlElement(name="LiftFeedRate")
	private double liftFeedRate;
    @XmlElement(name="LiftRetractRate")
	private double liftRetractRate;
    @XmlElement(name="ExportOption")
	private ExportOption exportOption;
    @XmlElement(name="FlipX")
	private boolean flipX;
    @XmlElement(name="FlipY")
	private boolean flipY;
    @XmlElement(name="Notes")
	private String notes;
	private String gCodeHeader;
	private String gCodeFooter;
	private String gCodePreslice;
	private String gCodeLift;
	private String zLiftSpeedGCode;
	private String zLiftDistanceGCode;
	@XmlElement(name="ZLiftDistanceCalculator")
	private String zLiftDistanceCalculator;
	@XmlElement(name="ZLiftSpeedCalculator")
	private String zLiftSpeedCalculator;
	@XmlElement(name="ProjectorGradientCalculator")
	private String projectorGradientCalculator;
	@XmlElement(name="ExposureTimeCalculator")
	private String exposureTimeCalculator;
    @XmlElement(name="SelectedInk")
	private String selectedInk;
    @XmlElement(name="MinTestExposure")
	private int minTestExposure;
    @XmlElement(name="TestExposureStep")
	private int testExposureStep;
    @XmlElement(name="InkConfig")
	private List<InkConfig> inkConfig;
	
	@XmlTransient
	public int getxResolution() {
		return xResolution;
	}
	public void setxResolution(int xResolution) {
		this.xResolution = xResolution;
	}
	
	@XmlTransient
	public int getyResolution() {
		return yResolution;
	}
	public void setyResolution(int yResolution) {
		this.yResolution = yResolution;
	}
	
	@XmlTransient
	public double getDotsPermmX() {
		return dotsPermmX;
	}
	public void setDotsPermmX(double dotsPermmX) {
		this.dotsPermmX = dotsPermmX;
	}
	
	@XmlTransient
	public double getDotsPermmY() {
		return dotsPermmY;
	}
	public void setDotsPermmY(double dotsPermmY) {
		this.dotsPermmY = dotsPermmY;
	}
	
	@XmlTransient
	public BuildDirection getDirection() {
		return direction;
	}
	public void setDirection(BuildDirection direction) {
		this.direction = direction;
	}
	
	@XmlTransient
	public double getLiftDistance() {
		return liftDistance;
	}
	public void setLiftDistance(double liftDistance) {
		this.liftDistance = liftDistance;
	}
	
	@XmlTransient
	public double getLiftFeedRate() {
		return liftFeedRate;
	}
	public void setLiftFeedRate(double liftFeedRate) {
		this.liftFeedRate = liftFeedRate;
	}
	
	@XmlTransient
	public boolean isFlipX() {
		return flipX;
	}
	public void setFlipX(boolean flipX) {
		this.flipX = flipX;
	}
	
	@XmlTransient
	public boolean isFlipY() {
		return flipY;
	}
	public void setFlipY(boolean flipY) {
		this.flipY = flipY;
	}
	
	@XmlTransient
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public String getgCodeHeader() {
		return gCodeHeader;
	}
	public void setgCodeHeader(String gCodeHeader) {
		this.gCodeHeader = TemplateEngine.convertToFreeMarkerTemplate(gCodeHeader);
	}
	
	public String getgCodeFooter() {
		return gCodeFooter;
	}
	public void setgCodeFooter(String gCodeFooter) {
		this.gCodeFooter = TemplateEngine.convertToFreeMarkerTemplate(gCodeFooter);
	}
	
	public String getgCodePreslice() {
		return gCodePreslice;
	}
	public void setgCodePreslice(String gCodePreslice) {
		this.gCodePreslice = TemplateEngine.convertToFreeMarkerTemplate(gCodePreslice);
	}
	
	public String getgCodeLift() {
		return gCodeLift;
	}
	public void setgCodeLift(String gCodeLift) {
		this.gCodeLift = TemplateEngine.convertToFreeMarkerTemplate(gCodeLift);
	}
	
	@XmlTransient
	public List<InkConfig> getInkConfigs() {
		return inkConfig;
	}
	public void setInkConfigs(List<InkConfig> inkConfig) {
		this.inkConfig = inkConfig;
	}
	
	@XmlTransient
	public String getSelectedInkConfigName() {
		return selectedInk;
	}
	public void setSelectedInkConfigName(String selectedInk) {
		this.selectedInk = selectedInk;
	}
	
	@JsonIgnore
	public InkConfig getSelectedInkConfig() {
		for (InkConfig currentInkConfig : inkConfig) {
			if (currentInkConfig.getName().equals(selectedInk)) {
				return currentInkConfig;
			}
		}
		
		return null;
	}
	
	public String getZLiftSpeedGCode() {
		return this.zLiftSpeedGCode;
	}
	public void setZLiftSpeedGCode(String gcodes) {
		this.zLiftSpeedGCode = TemplateEngine.convertToFreeMarkerTemplate(gcodes);
	}
	
	public String getZLiftDistanceGCode() {
		return this.zLiftDistanceGCode;
	}
	public void setZLiftDistanceGCode(String gcodes) {
		this.zLiftDistanceGCode = TemplateEngine.convertToFreeMarkerTemplate(gcodes);
	}
	
	@XmlTransient
	public String getzLiftDistanceCalculator() {
		return zLiftDistanceCalculator;
	}
	public void setzLiftDistanceCalculator(String zLiftDistanceCalculator) {
		this.zLiftDistanceCalculator = zLiftDistanceCalculator;
	}
	
	@XmlTransient
	public String getzLiftSpeedCalculator() {
		return zLiftSpeedCalculator;
	}
	public void setzLiftSpeedCalculator(String zLiftSpeedCalculator) {
		this.zLiftSpeedCalculator = zLiftSpeedCalculator;
	}
	
	@XmlTransient
	public String getProjectorGradientCalculator() {
		return projectorGradientCalculator;
	}
	public void setProjectorGradientCalculator(String projectorGradientCalculator) {
		this.projectorGradientCalculator = projectorGradientCalculator;
	}
	
	@XmlTransient
	public String getExposureTimeCalculator() {
		return exposureTimeCalculator;
	}
	public void setExposureTimeCalculator(String exposureTimeCalculator) {
		this.exposureTimeCalculator = exposureTimeCalculator;
	}
}
