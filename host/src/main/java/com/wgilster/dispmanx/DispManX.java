package com.wgilster.dispmanx;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

//https://github.com/raspberrypi/firmware/blob/master/opt/vc/include/bcm_host.h
//https://github.com/raspberrypi/userland/blob/master/interface/vmcs_host/vc_dispmanx.h
//https://github.com/raspberrypi/userland/blob/master/interface/vmcs_host/vc_dispmanx_types.h
public interface DispManX extends Library {
	public DispManX INSTANCE = (DispManX)Native.loadLibrary("bcm_host", DispManX.class);
	
	public int bcm_host_init();
	public int graphics_get_display_size(int screenIndex, IntByReference width, IntByReference height);
	public int vc_dispmanx_display_open(int screenIndex);
	public int vc_dispmanx_resource_create(int resourceType, int width, int height, IntByReference imagePointer);
	public int vc_dispmanx_rect_set(VC_RECT_T.ByReference rectangleToCreate, int offsetX, int offsetY, int width, int height);
	public int vc_dispmanx_update_start(int priority);
	public int vc_dispmanx_element_add(
			int updateHandle, 
			int displayHandle,
			int layer,
			VC_RECT_T.ByReference destinationRectangle, 
			int sourceResourceHandle, 
			VC_RECT_T.ByReference sourceRectangle,
			int protectionMode,
			VC_DISPMANX_ALPHA_T.ByReference alpha,
			int clamp,
			int imageTransformation);
	public int vc_dispmanx_update_submit_sync(int updateHandle);
	public int vc_dispmanx_update_submit(int updateHandle);
	public int vc_dispmanx_display_get_info(
			int displayHandle, 
			DISPMANX_MODEINFO_T pinfo);
	public int vc_dispmanx_resource_read_data(
            int resourceHandle,
            VC_RECT_T.ByReference p_rect,
            Pointer   dst_address,
            int dst_pitch );
	public int vc_dispmanx_resource_write_data(
			int resourceHandle,
			int resourceType,
			int pitch, 
			Pointer imageConvertedFromBufferedImage, 
			VC_RECT_T.ByReference copyRectangle);
	public int vc_dispmanx_element_remove(int updateHandle, int elementHandle);
	public int vc_dispmanx_resource_delete(int resourceHandle);
	public int vc_dispmanx_display_close(int displayHandle);
	public int vc_dispmanx_snapshot(
			int displayHandle,
			int resourceHandle,
            int transform);
}