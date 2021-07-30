package com.wgilster.dispmanx.window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.wgilster.dispmanx.DISPMANX_FLAGS_ALPHA_T;
import com.wgilster.dispmanx.DISPMANX_MODEINFO_T;
import com.wgilster.dispmanx.DispManX;
import com.wgilster.dispmanx.PROTECTION;
import com.wgilster.dispmanx.SCREEN;
import com.wgilster.dispmanx.VC_DISPMANX_ALPHA_T;
import com.wgilster.dispmanx.VC_IMAGE_TRANSFORM_T;
import com.wgilster.dispmanx.VC_IMAGE_TYPE_T;
import com.wgilster.dispmanx.VC_RECT_T;

public class Display {
	public static boolean hasInitialized;
	private int displayHandle;
	private int defaultLayer = 11000;//Retroarch looks like it's at layer 10,000 which means we need to be higher
	private DISPMANX_MODEINFO_T modeInfo;
	private List<DisplayListener> listeners = new ArrayList<DisplayListener>();
	
	public Display(DISPMANX_MODEINFO_T mode) {
		this.modeInfo = mode;
	}
	
	public Display(SCREEN screen) {
		if (!hasInitialized) {
			DispManX.INSTANCE.bcm_host_init();
			hasInitialized = true;
		}
		
		displayHandle = DispManX.INSTANCE.vc_dispmanx_display_open(screen.getId());
	}
	
	int getDisplayHandle() {
		return displayHandle;
	}
	
	public synchronized Resource createResource(NativeMemoryBackedBufferedImage image) {
//System.out.println(new Date() + ": createResourceStart->" + image);
        VC_RECT_T.ByReference destinationRect = new VC_RECT_T.ByReference();
        DispManX.INSTANCE.vc_dispmanx_rect_set(destinationRect, 0, 0, image.getWidth(), image.getHeight());


        IntByReference ref = new IntByReference();
        int resourceHandle = DispManX.INSTANCE.vc_dispmanx_resource_create( 
        		VC_IMAGE_TYPE_T.VC_IMAGE_ARGB8888.getId(), 
        		image.getWidth(), 
        		image.getHeight(), 
        		ref );
        if (resourceHandle <= 0) {
			throw new OutOfMemoryError("You probably ran out of GPU memory in your Raspberry Pi while attempting to create a resource.");
        }
        if (DispManX.INSTANCE.vc_dispmanx_resource_write_data(
        		resourceHandle, 
        		VC_IMAGE_TYPE_T.VC_IMAGE_ARGB8888.getId(), 
        		image.getPitch(), 
        		image.getMemory(),
        		destinationRect 
        	) != 0) {
        	DispManX.INSTANCE.vc_dispmanx_resource_delete(resourceHandle);
			throw new OutOfMemoryError("You probably ran out of GPU memory in your Raspberry Pi while attempting to create a resource.");
        }
//System.out.println(new Date() + ": createResourceEnd->" + image);
        return new Resource(image, resourceHandle);
	}
	
	public synchronized DISPMANX_MODEINFO_T getScreenInfo() {
		if (modeInfo == null) {
		    modeInfo = DISPMANX_MODEINFO_T.ByReference.newInstance(DISPMANX_MODEINFO_T.class);
		    if (DispManX.INSTANCE.vc_dispmanx_display_get_info(displayHandle, modeInfo) != 0) {
		    	throw new RuntimeException("Failure getting display info");
		    }
		    modeInfo.autoRead();
		}
	    return modeInfo;
	}
	
	public synchronized void close() {
        DispManX.INSTANCE.vc_dispmanx_display_close(displayHandle);
        for (DisplayListener listener : listeners) {
        	listener.close();
        }
	}
	
	public void addDisplayListener(DisplayListener listener) {
		listeners.add(listener);
	}
	
	public void removeDisplayListener(DisplayListener listener) {
		listeners.remove(listener);
	}
	
    public synchronized NativeMemoryBackedBufferedImage getScreenShot() {
    	DISPMANX_MODEINFO_T info = getScreenInfo();
    	int width = info.width;
    	int height = info.height;
	    int resource = DispManX.INSTANCE.vc_dispmanx_resource_create(VC_IMAGE_TYPE_T.VC_IMAGE_ARGB8888.getId(), width, height, new IntByReference());
	    int bytesPerPixel = 4;
	    int dmxPitch = bytesPerPixel * ((width + 15) & ~15);
	    Memory dmxImagePtr = new Memory(dmxPitch * height);
	    DispManX.INSTANCE.vc_dispmanx_snapshot(getDisplayHandle(), resource, 0);
	    VC_RECT_T.ByReference clip = VC_RECT_T.ByReference.newInstance(VC_RECT_T.ByReference.class);
	    DispManX.INSTANCE.vc_dispmanx_rect_set(clip, 0, 0, width, height);
	    DispManX.INSTANCE.vc_dispmanx_resource_read_data(resource, clip, dmxImagePtr, dmxPitch);
	    DispManX.INSTANCE.vc_dispmanx_resource_delete(resource);
	    return NativeMemoryBackedBufferedImage.newInstance(width, height, bytesPerPixel, dmxImagePtr);
    }
    
    public synchronized NativeMemoryBackedBufferedImage getScreenClip(int x, int y, int width, int height) {
	    int resource = DispManX.INSTANCE.vc_dispmanx_resource_create(VC_IMAGE_TYPE_T.VC_IMAGE_ARGB8888.getId(), width, height, new IntByReference());
	    int bytesPerPixel = 4;
	    int dmxPitch = bytesPerPixel * ((width + 15) & ~15);
	    Memory dmxImagePtr = new Memory(dmxPitch * height);
	    DispManX.INSTANCE.vc_dispmanx_snapshot(getDisplayHandle(), resource, 0);
	    VC_RECT_T.ByReference clip = VC_RECT_T.ByReference.newInstance(VC_RECT_T.ByReference.class);
	    DispManX.INSTANCE.vc_dispmanx_rect_set(clip, x, y, width, height);
	    DispManX.INSTANCE.vc_dispmanx_resource_read_data(resource, clip, dmxImagePtr, dmxPitch);
	    DispManX.INSTANCE.vc_dispmanx_resource_delete(resource);
	    return NativeMemoryBackedBufferedImage.newInstance(width, height, bytesPerPixel, dmxImagePtr);
    }
    
    public static void main(String[] args) throws IOException {
    	Display display = new Display(SCREEN.DISPMANX_ID_MAIN_LCD);
		NativeMemoryBackedBufferedImage image = display.getScreenShot();
		ImageIO.write(image, "PNG", new File("Test.png"));
	}
    
	public synchronized Sprite showNow(Resource resourceHandle, int x, int y, int layer) {
//System.out.println(new Date() + ": showNowStart->" + resourceHandle);
        int update = DispManX.INSTANCE.vc_dispmanx_update_start( 0 );
        VC_RECT_T.ByReference destinationRect = new VC_RECT_T.ByReference();
        DispManX.INSTANCE.vc_dispmanx_rect_set(destinationRect, x, y, resourceHandle.getWidth(), resourceHandle.getHeight());
        VC_RECT_T.ByReference sourceRect = new VC_RECT_T.ByReference();
        DispManX.INSTANCE.vc_dispmanx_rect_set( sourceRect, 0, 0, resourceHandle.getWidth()<<16, resourceHandle.getHeight()<<16);
        
        VC_DISPMANX_ALPHA_T.ByReference alpha = new VC_DISPMANX_ALPHA_T.ByReference();
        alpha.flags = DISPMANX_FLAGS_ALPHA_T.DISPMANX_FLAGS_ALPHA_FROM_SOURCE.getId();
        alpha.opacity = 255;
        
        int element = DispManX.INSTANCE.vc_dispmanx_element_add(
        		update, 
        		displayHandle, 
        		defaultLayer + layer, 
        		destinationRect, 
        		resourceHandle.getResourceHandle(), 
        		sourceRect, 
        		PROTECTION.DISPMANX_PROTECTION_NONE.getId(), 
        		alpha, 
        		0, 
        		VC_IMAGE_TRANSFORM_T.VC_IMAGE_ROT0.getId() );
        
        DispManX.INSTANCE.vc_dispmanx_update_submit_sync( update );
//System.out.println(new Date() + ": showNowEnd->" + resourceHandle);
        return new Sprite(resourceHandle, element);
	}
	
	public synchronized void removeNow(Sprite sprite) {
		if (sprite == null) {
			throw new NullPointerException("Sprite can't be null!");
		}
		int update = DispManX.INSTANCE.vc_dispmanx_update_start( 0 );
        DispManX.INSTANCE.vc_dispmanx_element_remove( update, sprite.getElementHandle());
        DispManX.INSTANCE.vc_dispmanx_update_submit_sync( update );
	}
	
	public synchronized Sprite animate(Sprite removeElement, Resource showResourceHandle, int x, int y, int layer) {
//System.out.println(new Date() + ": animateStart->" + showResourceHandle);
        int update = DispManX.INSTANCE.vc_dispmanx_update_start( 0 );
        VC_RECT_T.ByReference destinationRect = new VC_RECT_T.ByReference();
        DispManX.INSTANCE.vc_dispmanx_rect_set(destinationRect, x, y, showResourceHandle.getWidth(), showResourceHandle.getHeight());
        VC_RECT_T.ByReference sourceRect = new VC_RECT_T.ByReference();
        DispManX.INSTANCE.vc_dispmanx_rect_set( sourceRect, 0, 0, showResourceHandle.getWidth()<<16, showResourceHandle.getHeight()<<16);
        
        VC_DISPMANX_ALPHA_T.ByReference alpha = new VC_DISPMANX_ALPHA_T.ByReference();
        alpha.flags = DISPMANX_FLAGS_ALPHA_T.DISPMANX_FLAGS_ALPHA_FROM_SOURCE.getId();
        alpha.opacity = 255;
        
        DispManX.INSTANCE.vc_dispmanx_element_remove(
        		update, 
        		removeElement.getElementHandle());
        int element = DispManX.INSTANCE.vc_dispmanx_element_add(
        		update, 
        		displayHandle, 
        		defaultLayer + layer, 
        		destinationRect, 
        		showResourceHandle.getResourceHandle(), 
        		sourceRect, 
        		PROTECTION.DISPMANX_PROTECTION_NONE.getId(), 
        		alpha, 
        		0, 
        		VC_IMAGE_TRANSFORM_T.VC_IMAGE_ROT0.getId() );
        
        DispManX.INSTANCE.vc_dispmanx_update_submit_sync( update );
//System.out.println(new Date() + ": animateEnd->" + showResourceHandle);
        return new Sprite(showResourceHandle, element);
	}
	
	public synchronized Sprite animate(Sprite currentSprite, NativeMemoryBackedBufferedImage image, int x, int y, int layer) {
		Sprite oldSprite = currentSprite;
		Resource resource = createResource(image);
		currentSprite = animate(currentSprite, resource, x, y, layer);
		oldSprite.getResource().release();
		return currentSprite;
	}
}
