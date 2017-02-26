package org.area515.resinprinter.display;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.area515.resinprinter.display.dispmanx.ALPHA;
import org.area515.resinprinter.display.dispmanx.DispManX;
import org.area515.resinprinter.display.dispmanx.PROTECTION;
import org.area515.resinprinter.display.dispmanx.SCREEN;
import org.area515.resinprinter.display.dispmanx.VC_DISPMANX_ALPHA_T;
import org.area515.resinprinter.display.dispmanx.VC_IMAGE_TRANSFORM_T;
import org.area515.resinprinter.display.dispmanx.VC_IMAGE_TYPE_T;
import org.area515.resinprinter.display.dispmanx.VC_RECT_T;

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;

public class DispManXLoadImage {
	public static void displayInfo( int id, String name ) {
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        int res = DispManX.INSTANCE.graphics_get_display_size( id, width, height );
        if ( res >= 0 ) {
            System.out.printf( "\t%d %s: %dx%d\n", id, name, width.getValue(), height.getValue() );
        } else {
        	System.out.println("graphics_get_display_size returned:" + res);
        }
	}
	
	public static void usage() {
		System.out.printf( "usage: org.area515.resinprinter.display.DispManXTest <display> <duration in seconds> <file>\n");
		displayInfo( SCREEN.DISPMANX_ID_MAIN_LCD.getId(), "Main LCD" );
		displayInfo( SCREEN.DISPMANX_ID_AUX_LCD.getId(), "AUX LCD" );
		displayInfo( SCREEN.DISPMANX_ID_HDMI.getId(), "HDMI" );
		displayInfo( SCREEN.DISPMANX_ID_SDTV.getId(), "SDTV" );
		displayInfo( SCREEN.DISPMANX_ID_FORCE_LCD.getId(), "Force LCD" );
		displayInfo( SCREEN.DISPMANX_ID_FORCE_TV.getId(), "Force TV" );
		displayInfo( SCREEN.DISPMANX_ID_FORCE_OTHER.getId(), "Force other" );
		System.exit( 1 );
	}
	
	public static void info( int screen ) {
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        int res = DispManX.INSTANCE.graphics_get_display_size( screen, width, height );
        if (res >= 0) {
        	System.out.printf( "%d, %d\n", width, height );
        } else {
        	System.out.println("graphics_get_display_size returned:" + res);
        }
		System.exit( 1 );
	}
	
	public static void res( String str, int val ) {
		if ( val != 0 ) {
			System.out.printf( "%s: %08x\n", str, val );
		} else {
			System.out.println(str + " is 0");
		}
	}
	
	public static int getPitch( int x, int y ) {
	        return ((x + (y)-1) & ~((y)-1));//y*((x + y-1)/y);
	}
	
	public static Memory loadBitmapRGB565(String fileName, IntByReference width, IntByReference height, IntByReference pitchByRef) throws IOException {
		int bytesPerPixel = 2;
		BufferedImage image = ImageIO.read(new File(fileName));
		int pitch = getPitch( bytesPerPixel * image.getWidth(), 32 );
		pitchByRef.setValue(pitch);
		Memory destPixels = new Memory(pitch * image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
        		int rgb = image.getRGB(x, y);
        		destPixels.setShort((y*(pitch / bytesPerPixel) + x) * bytesPerPixel, (short)(((rgb & 0xf80000) >>> 8) | ((rgb & 0xfc00) >>> 5) | (rgb & 0xf8 >>> 3)));
            }
        }
        width.setValue(image.getWidth());
        height.setValue(image.getHeight());
        return destPixels;
	}
	
	public static Memory loadBitmapARGB8888(String fileName, IntByReference width, IntByReference height, IntByReference pitchByRef) throws IOException {
		int bytesPerPixel = 4;
		BufferedImage image = ImageIO.read(new File(fileName));
		int pitch = getPitch( bytesPerPixel * image.getWidth(), 32 );
		pitchByRef.setValue(pitch);
		long start = System.currentTimeMillis();
		Memory destPixels = new Memory(pitch * image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
        		destPixels.setInt((y*(pitch / bytesPerPixel) + x) * bytesPerPixel, image.getRGB(x, y));
            }
        }
        System.out.println(System.currentTimeMillis() - start);
        width.setValue(image.getWidth());
        height.setValue(image.getHeight());
        return destPixels;
	}
	
	//sudo vi DispManXLoadImage.java
	//sudo javac -cp lib/*:. DispManXLoadImage.java
	//sudo java -cp lib/*:. DispManXLoadImage 2 10 resourcesnew/favicon/apple-icon-144x144.png
	public static void main(String[] args) throws IOException, InterruptedException {
		DispManX dispMan = DispManX.INSTANCE;
		System.out.println("BCM Initialized:" + dispMan.bcm_host_init());
        if ( args.length < 3 ) {
        	usage();
        	return;
        }
        
        int screen = Integer.parseInt( args[0] );
        /*if (screen == 2 || screen == 3 || screen == 5) {
        	IntByReference vchiHandle = new IntByReference();
        	PointerByReference connections = new PointerByReference();

        	dispMan.vcos_init();
        	res("vchi_initialise", dispMan.vchi_initialise(vchiHandle));
        	res("vchi_connect", dispMan.vchi_connect(null, 0, vchiHandle.getValue()));
        	res("vc_vchi_tv_init", dispMan.vc_vchi_tv_init(vchiHandle, connections.getPointer(), 1));
        	res("vc_tv_hdmi_power_on_preferred", dispMan.vc_tv_hdmi_power_on_preferred());
        }*/
        
        int time = Integer.parseInt( args[1] );
        IntByReference pitch = new IntByReference();
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        res( "get display size", dispMan.graphics_get_display_size( screen, width, height ) );
        System.out.printf( "display %d: %d x %d\n", screen, width.getValue(), height.getValue() );
        int display = dispMan.vc_dispmanx_display_open( screen );
        Memory bitmap = loadBitmapRGB565( args[2], width, height, pitch );
        System.out.printf( "bitmap: %d x %d pitch->%d\n", width.getValue(), height.getValue(), pitch.getValue());

        VC_RECT_T.ByReference copyRect = new VC_RECT_T.ByReference();
        VC_RECT_T.ByReference sourceRect = new VC_RECT_T.ByReference();
        VC_RECT_T.ByReference destinationRect = new VC_RECT_T.ByReference();
        
        res( "rect set", dispMan.vc_dispmanx_rect_set( copyRect, 0, 0, width.getValue(), height.getValue() ) );
        //This seems to be some form of a zoom factor
        res( "rect set", dispMan.vc_dispmanx_rect_set( sourceRect, 0, 0, width.getValue()<<16, height.getValue()<<16 ) );
        res( "rect set", dispMan.vc_dispmanx_rect_set( destinationRect, 0, 0, width.getValue(), height.getValue() ) );

        IntByReference ref = new IntByReference();
        int resourceHandle = dispMan.vc_dispmanx_resource_create( 
        		VC_IMAGE_TYPE_T.VC_IMAGE_RGB565.getcIndex(), 
        		width.getValue(), 
        		height.getValue(), 
        		ref );
        res( "resource write data", dispMan.vc_dispmanx_resource_write_data( 
        		resourceHandle, 
        		VC_IMAGE_TYPE_T.VC_IMAGE_RGB565.getcIndex(), 
        		pitch.getValue() , 
        		bitmap, 
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
