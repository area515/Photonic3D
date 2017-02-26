package org.area515.resinprinter.display;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;

import org.area515.resinprinter.display.dispmanx.ALPHA;
import org.area515.resinprinter.display.dispmanx.DispManX;
import org.area515.resinprinter.display.dispmanx.NativeMemoryBackedBufferedImage;
import org.area515.resinprinter.display.dispmanx.PROTECTION;
import org.area515.resinprinter.display.dispmanx.VC_DISPMANX_ALPHA_T;
import org.area515.resinprinter.display.dispmanx.VC_IMAGE_TRANSFORM_T;
import org.area515.resinprinter.display.dispmanx.VC_IMAGE_TYPE_T;
import org.area515.resinprinter.display.dispmanx.VC_RECT_T;

import com.sun.jna.ptr.IntByReference;

public class DispManXDirectMemory {
	public static void res( String str, int val ) {
		if ( val != 0 ) {
			System.out.printf( "%s: %08x\n", str, val );
		} else {
			System.out.println(str + " is 0");
		}
	}
	
	//sudo vi DispManXDirectMemory.java
	//sudo javac -cp lib/*:. DispManXDirectMemory.java
	//sudo java -cp lib/*:. DispManXDirectMemory 2 10
	public static void main(String[] args) throws IOException, InterruptedException {
		DispManX dispMan = DispManX.INSTANCE;
		System.out.println("BCM Initialized:" + dispMan.bcm_host_init());
        
        int screen = Integer.parseInt( args[0] );
        int time = Integer.parseInt( args[1] );
        //IntByReference pitch = new IntByReference();
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        res( "get display size", dispMan.graphics_get_display_size( screen, width, height ) );
        System.out.printf( "display %d: %d x %d\n", screen, width.getValue(), height.getValue() );
        int display = dispMan.vc_dispmanx_display_open( screen );
        
        NativeMemoryBackedBufferedImage data = NativeMemoryBackedBufferedImage.newInstance(width.getValue(), height.getValue());
        int pitch = data.getPitch();
        Graphics2D g = (Graphics2D)data.createGraphics();
        g.drawOval(0,  0, width.getValue(), height.getValue());
        g.setColor(Color.green);
        g.setFont(g.getFont().deriveFont((float)30));
        System.out.println(g.getFont());
        g.drawString("Hello this is a test", width.getValue() / 2, 50);
        System.out.printf( "bitmap: %d x %d pitch->%d\n", width.getValue(), height.getValue(), pitch);

        VC_RECT_T.ByReference copyRect = new VC_RECT_T.ByReference();
        VC_RECT_T.ByReference sourceRect = new VC_RECT_T.ByReference();
        VC_RECT_T.ByReference destinationRect = new VC_RECT_T.ByReference();
        
        res( "rect set", dispMan.vc_dispmanx_rect_set( copyRect, 0, 0, width.getValue(), height.getValue() ) );
        //This seems to be some form of a zoom factor
        res( "rect set", dispMan.vc_dispmanx_rect_set( sourceRect, 0, 0, width.getValue()<<16, height.getValue()<<16 ) );
        res( "rect set", dispMan.vc_dispmanx_rect_set( destinationRect, 0, 0, width.getValue(), height.getValue() ) );

        IntByReference ref = new IntByReference();
        int resourceHandle = dispMan.vc_dispmanx_resource_create( 
        		VC_IMAGE_TYPE_T.VC_IMAGE_ARGB8888.getcIndex(), 
        		width.getValue(), 
        		height.getValue(), 
        		ref );
        res( "resource write data", dispMan.vc_dispmanx_resource_write_data( 
        		resourceHandle, 
        		VC_IMAGE_TYPE_T.VC_IMAGE_ARGB8888.getcIndex(), 
        		pitch, 
        		data.getMemory(), 
        		destinationRect )
        	);

        System.out.println("copyRect:" + copyRect.width + ", " + copyRect.height);
        System.out.println("sourceRect:" + sourceRect.width + ", " + sourceRect.height);
        System.out.println("destinationRect:" + destinationRect.width + ", " + destinationRect.height);
        
        int update = dispMan.vc_dispmanx_update_start( 0 );
        VC_DISPMANX_ALPHA_T.ByReference alpha = new VC_DISPMANX_ALPHA_T.ByReference();
        alpha.flags = ALPHA.DISPMANX_FLAGS_ALPHA_FROM_SOURCE.getFlag() | ALPHA.DISPMANX_FLAGS_ALPHA_FIXED_ALL_PIXELS.getFlag();
        alpha.opacity = 255;
        

        int element = dispMan.vc_dispmanx_element_add(
        		update, 
        		display, 
        		2010, 
        		destinationRect, 
        		resourceHandle, 
        		sourceRect, 
        		PROTECTION.DISPMANX_PROTECTION_NONE.getcConst(), 
        		alpha, 
        		0, 
        		VC_IMAGE_TRANSFORM_T.VC_IMAGE_ROT0.getcConst() );
        
        res( "submit", dispMan.vc_dispmanx_update_submit_sync( update ) );

        Thread.sleep( time * 1000 );
        update = dispMan.vc_dispmanx_update_start( 0 );

        res( "element remove", dispMan.vc_dispmanx_element_remove( update, element ) );
        res( "submit", dispMan.vc_dispmanx_update_submit_sync( update ) );
        res( "resource delete", dispMan.vc_dispmanx_resource_delete( resourceHandle ) );
        res( "display close", dispMan.vc_dispmanx_display_close( display ) );
	}
}
