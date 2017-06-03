package org.area515.resinprinter.printphoto.micoin;

import java.io.File;

import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.job.render.CurrentImageRenderer;
import org.area515.resinprinter.printphoto.ImagePrintFileProcessor;
import org.area515.resinprinter.twodim.TwoDimensionalImageRenderer;

public class CoinFileProcessor extends ImagePrintFileProcessor {
	@Override
	public String[] getFileExtensions() {
		return new String[]{"coin"};
	}

	@Override
	public boolean acceptsFile(File processingFile) {
		//TODO: this could be smarter by loading the file instead of just checking the file type
		String name = processingFile.getName().toLowerCase();
		return name.endsWith("coin");
	}

	@Override
	public String getFriendlyName() {
		return "Coin";
	}
	
	@Override
	protected CurrentImageRenderer buildPlatformRenderer(DataAid aid, Object nextRenderingPointer, int totalPlatformSlices, CurrentImageRenderer platformSizeInitializer) {
		return new CoinRenderer(aid, this, nextRenderingPointer);
	}

	@Override
	public TwoDimensionalImageRenderer createTwoDimensionalRenderer(DataAid aid, Object imageIndexToBuild) {
		return new CoinRenderer(aid, this, imageIndexToBuild);
	}
	
	//Double our extrusion count because head = extrusion1; middle = platform; tails = extrusion2;
	@Override
	protected void setupSlices(PrintJob printJob, TwoDimensionalDataAid dataAid, int suggestedPlatformSlices, int suggestedExtrusionSlices) {
		super.setupSlices(printJob, dataAid, suggestedPlatformSlices, suggestedExtrusionSlices * 2);
	}
}
