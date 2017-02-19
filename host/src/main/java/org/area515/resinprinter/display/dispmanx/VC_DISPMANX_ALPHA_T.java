package org.area515.resinprinter.display.dispmanx;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

public class VC_DISPMANX_ALPHA_T extends Structure {
	public static class ByReference extends VC_DISPMANX_ALPHA_T implements Structure.ByReference {}
	
	public int flags;
	public int opacity;
	public int mask;
	
	@Override
	protected List<String> getFieldOrder() {
		return Arrays.asList("flags", "opacity", "mask");
	}
}