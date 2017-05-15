package org.area515.resinprinter.printphoto.micoin;

import java.io.File;

import org.area515.resinprinter.job.AbstractPrintFileProcessor;
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
	protected CurrentImageRenderer buildPlatformRenderer(DataAid aid, Object nextRenderingPointer, int totalPlatformSlices, TwoDimensionalImageRenderer platformSizeInitializer) {
		return new CoinRenderer(aid, this, nextRenderingPointer);
	}

	@Override
	public TwoDimensionalImageRenderer createRenderer(DataAid aid, AbstractPrintFileProcessor<?, ?> processor, Object imageIndexToBuild) {
		return new CoinRenderer(aid, processor, imageIndexToBuild);
	}
}
