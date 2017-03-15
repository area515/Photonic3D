package org.area515.resinprinter.display.dispmanx;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

public class VC_RECT_T extends Structure {
	public static class ByReference extends VC_RECT_T implements Structure.ByReference {}
	   public int x;
	   public int y;
	   public int width;
	   public int height;

    @Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("x", "y", "width", "height");
	}
}