package com.wgilster.dispmanx;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({"width", "height", "transform", "input_format", "display_num"})
public class DISPMANX_MODEINFO_T extends Structure {
	public static class ByReference extends DISPMANX_MODEINFO_T implements Structure.ByReference {}

	public int width;
	public int height;
	public int transform;//Unknown type
	public int input_format;//Unknown type
	public int display_num;
}
