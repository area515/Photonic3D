package org.area515.resinprinter.display.dispmanx;

import org.area515.resinprinter.display.DispManXTest.VC_DISPMANX_ALPHA_T;
import org.area515.resinprinter.display.DispManXTest.VC_RECT_T;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public interface DispManX extends Library {
	public DispManX INSTANCE = (DispManX)Native.loadLibrary("bcm_host", DispManX.class);
	
	public int bcm_host_init();
	public int graphics_get_display_size(int screenIndex, IntByReference width, IntByReference height);
	public int vc_dispmanx_display_open(int screenIndex);
	public int vc_dispmanx_resource_create(int resourceType, int width, int height, IntByReference imagePointer);
	public int vc_dispmanx_rect_set(VC_RECT_T.ByReference rectangleToCreate, int offsetX, int offsetY, int width, int height);
	public int vc_dispmanx_update_start(int priority);//Constant of 10
	public int vc_dispmanx_element_add(
			int updateHandle, 
			int displayHandle,
			int layer,                            // 2000
			VC_RECT_T.ByReference destinationRectangle, 
			int sourceResourceHandle, 
			VC_RECT_T.ByReference sourceRectangle,
			int protectionMode,                          // DISPMANX_PROTECTION_NONE
			VC_DISPMANX_ALPHA_T.ByReference alpha,       // { DISPMANX_FLAGS_ALPHA_FROM_SOURCE | DISPMANX_FLAGS_ALPHA_FIXED_ALL_PIXELS, 255, 0 };
			int clamp,                            // 0
			int imageTransformation);                    // VC_IMAGE_ROT0 
	public int vc_dispmanx_update_submit_sync(int updateHandle);
	public int vc_dispmanx_resource_write_data(
			int resourceHandle,
			int resourceType,                            // VC_IMAGE_RGB565
			int pitch, 
			Pointer imageConvertedFromBufferedImage, 
			VC_RECT_T.ByReference copyRectangle);
	public int vc_dispmanx_element_remove(int updateHandle, int elementHandle);
	public int vc_dispmanx_resource_delete(int resourceHandle);
	public int vc_dispmanx_display_close(int displayHandle);
}