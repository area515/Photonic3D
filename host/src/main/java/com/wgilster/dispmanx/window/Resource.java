package com.wgilster.dispmanx.window;

import com.wgilster.dispmanx.DispManX;

public class Resource {
	private int resourceHandle;
	private NativeMemoryBackedBufferedImage image; 
	
	Resource(NativeMemoryBackedBufferedImage image, int resourceHandle) {
		this.image = image;
		this.resourceHandle = resourceHandle;
	}
	
	public int getWidth() {
		return image.getWidth();
	}
	
	public int getHeight() {
		return image.getHeight();
	}
	
	int getResourceHandle() {
		return resourceHandle;
	}
	
	/*@Override
	protected void finalize() throws Throwable {
		release();
		super.finalize();
	}*/
	
	public void release() {
		DispManX.INSTANCE.vc_dispmanx_resource_delete( resourceHandle );
	}
}
