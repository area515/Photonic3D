package org.area515.resinprinter.display;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

public class DispManXTestSelfContained {
	public static class VC_DISPMANX_ALPHA_T extends Structure {
		public static class ByReference extends VC_DISPMANX_ALPHA_T implements Structure.ByReference {}
		
		public int flags;
		public int opacity;
		public int mask;
		
		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("flags", "opacity", "mask");
		}
	}
	
	public static class VC_RECT_T extends Structure {
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

	public enum ALPHA {	
		DISPMANX_FLAGS_ALPHA_FROM_SOURCE(0),
		DISPMANX_FLAGS_ALPHA_FIXED_ALL_PIXELS(1),
		DISPMANX_FLAGS_ALPHA_FIXED_NON_ZERO(2),
		DISPMANX_FLAGS_ALPHA_FIXED_EXCEED_0X07(3),
		DISPMANX_FLAGS_ALPHA_PREMULT(1 << 16),
		DISPMANX_FLAGS_ALPHA_MIX(1 << 17);
		private int flag = 0;
		
		ALPHA(int flag) {
			this.flag = flag;
		}

		public int getFlag() {
			return flag;
		}
	}
	
	public enum TRANSFORM {
		TRANSFORM_HFLIP(1<<0),
		TRANSFORM_VFLIP(1<<1),
		TRANSFORM_TRANSPOSE (1<<2);
		private int cConst;
		
		TRANSFORM(int cConst) {
			this.cConst = cConst;
		}
		
		public int getcConst() {
			return cConst;
		}
	}
	
	public enum VC_IMAGE_TRANSFORM_T {
		VC_IMAGE_ROT0(0),
		VC_IMAGE_MIRROR_ROT0(TRANSFORM.TRANSFORM_HFLIP.getcConst()),
		VC_IMAGE_MIRROR_ROT180(TRANSFORM.TRANSFORM_VFLIP.getcConst()),
		VC_IMAGE_ROT180(TRANSFORM.TRANSFORM_HFLIP.getcConst()|TRANSFORM.TRANSFORM_VFLIP.getcConst()),
		VC_IMAGE_MIRROR_ROT90(TRANSFORM.TRANSFORM_TRANSPOSE.getcConst()),
		VC_IMAGE_ROT270(TRANSFORM.TRANSFORM_TRANSPOSE.getcConst()|TRANSFORM.TRANSFORM_HFLIP.getcConst()),
		VC_IMAGE_ROT90(TRANSFORM.TRANSFORM_TRANSPOSE.getcConst()|TRANSFORM.TRANSFORM_VFLIP.getcConst()),
		VC_IMAGE_MIRROR_ROT270(TRANSFORM.TRANSFORM_TRANSPOSE.getcConst()|TRANSFORM.TRANSFORM_HFLIP.getcConst()|TRANSFORM.TRANSFORM_VFLIP.getcConst());
		private int cConst;
	   
		VC_IMAGE_TRANSFORM_T(int cConst) {
			this.cConst = cConst;
		}
	   
		public int getcConst() {
			return cConst;
		}
	}
	
	public enum VC_IMAGE_TYPE_T {
	   VC_IMAGE_MIN(0), //bounds for error checking
	
	   VC_IMAGE_RGB565(1),
	   VC_IMAGE_1BPP(2),
	   VC_IMAGE_YUV420(3),
	   VC_IMAGE_48BPP(4),
	   VC_IMAGE_RGB888(5),
	   VC_IMAGE_8BPP(6),
	   VC_IMAGE_4BPP(7),    // 4bpp palettised image
	   VC_IMAGE_3D32(8),    /* A separated format of 16 colour/light shorts followed by 16 z values */
	   VC_IMAGE_3D32B(9),   /* 16 colours followed by 16 z values */
	   VC_IMAGE_3D32MAT(10), /* A separated format of 16 material/colour/light shorts followed by 16 z values */
	   VC_IMAGE_RGB2X9(11),   /* 32 bit format containing 18 bits of 6.6.6 RGB, 9 bits per short */
	   VC_IMAGE_RGB666(12),   /* 32-bit format holding 18 bits of 6.6.6 RGB */
	   VC_IMAGE_PAL4_OBSOLETE(13),     // 4bpp palettised image with embedded palette
	   VC_IMAGE_PAL8_OBSOLETE(14),     // 8bpp palettised image with embedded palette
	   VC_IMAGE_RGBA32(15),   /* RGB888 with an alpha byte after each pixel */ /* xxx: isn't it BEFORE each pixel? */
	   VC_IMAGE_YUV422(16),   /* a line of Y (32-byte padded), a line of U (16-byte padded), and a line of V (16-byte padded) */
	   VC_IMAGE_RGBA565(17),  /* RGB565 with a transparent patch */
	   VC_IMAGE_RGBA16(18),   /* Compressed (4444) version of RGBA32 */
	   VC_IMAGE_YUV_UV(19),   /* VCIII codec format */
	   VC_IMAGE_TF_RGBA32(20), /* VCIII T-format RGBA8888 */
	   VC_IMAGE_TF_RGBX32(21),  /* VCIII T-format RGBx8888 */
	   VC_IMAGE_TF_FLOAT(22), /* VCIII T-format float */
	   VC_IMAGE_TF_RGBA16(23), /* VCIII T-format RGBA4444 */
	   VC_IMAGE_TF_RGBA5551(24), /* VCIII T-format RGB5551 */
	   VC_IMAGE_TF_RGB565(25), /* VCIII T-format RGB565 */
	   VC_IMAGE_TF_YA88(26), /* VCIII T-format 8-bit luma and 8-bit alpha */
	   VC_IMAGE_TF_BYTE(27), /* VCIII T-format 8 bit generic sample */
	   VC_IMAGE_TF_PAL8(28), /* VCIII T-format 8-bit palette */
	   VC_IMAGE_TF_PAL4(29), /* VCIII T-format 4-bit palette */
	   VC_IMAGE_TF_ETC1(30), /* VCIII T-format Ericsson Texture Compressed */
	   VC_IMAGE_BGR888(31),  /* RGB888 with R & B swapped */
	   VC_IMAGE_BGR888_NP(32),  /* RGB888 with R & B swapped, but with no pitch, i.e. no padding after each row of pixels */
	   VC_IMAGE_BAYER(33),  /* Bayer image, extra defines which variant is being used */
	   VC_IMAGE_CODEC(34),  /* General wrapper for codec images e.g. JPEG from camera */
	   VC_IMAGE_YUV_UV32(35),   /* VCIII codec format */
	   VC_IMAGE_TF_Y8(36),   /* VCIII T-format 8-bit luma */
	   VC_IMAGE_TF_A8(37),   /* VCIII T-format 8-bit alpha */
	   VC_IMAGE_TF_SHORT(38),/* VCIII T-format 16-bit generic sample */
	   VC_IMAGE_TF_1BPP(39), /* VCIII T-format 1bpp black/white */
	   VC_IMAGE_OPENGL(40),
	   VC_IMAGE_YUV444I(41), /* VCIII-B0 HVS YUV 4:4:4 interleaved samples */
	   VC_IMAGE_YUV422PLANAR(42),  /* Y, U, & V planes separately (VC_IMAGE_YUV422 has them interleaved on a per line basis) */
	   VC_IMAGE_ARGB8888(43),   /* 32bpp with 8bit alpha at MS byte, with R, G, B (LS byte) */
	   VC_IMAGE_XRGB8888(44),   /* 32bpp with 8bit unused at MS byte, with R, G, B (LS byte) */
	
	   VC_IMAGE_YUV422YUYV(45),  /* interleaved 8 bit samples of Y, U, Y, V */
	   VC_IMAGE_YUV422YVYU(46),  /* interleaved 8 bit samples of Y, V, Y, U */
	   VC_IMAGE_YUV422UYVY(47),  /* interleaved 8 bit samples of U, Y, V, Y */
	   VC_IMAGE_YUV422VYUY(48),  /* interleaved 8 bit samples of V, Y, U, Y */
	
	   VC_IMAGE_RGBX32(49),      /* 32bpp like RGBA32 but with unused alpha */
	   VC_IMAGE_RGBX8888(50),    /* 32bpp, corresponding to RGBA with unused alpha */
	   VC_IMAGE_BGRX8888(51),    /* 32bpp, corresponding to BGRA with unused alpha */
	
	   VC_IMAGE_YUV420SP(52),    /* Y as a plane, then UV byte interleaved in plane with with same pitch, half height */
	   
	   VC_IMAGE_YUV444PLANAR(53),  /* Y, U, & V planes separately 4:4:4 */
	
	   VC_IMAGE_TF_U8(54),   /* T-format 8-bit U - same as TF_Y8 buf from U plane */
	   VC_IMAGE_TF_V8(55),   /* T-format 8-bit U - same as TF_Y8 buf from V plane */
	   
	   VC_IMAGE_MAX(56),     //bounds for error checking
	   VC_IMAGE_FORCE_ENUM_16BIT(0xffff);
	   
	   private int cIndex;
	   
	   VC_IMAGE_TYPE_T(int cIndex) {
		   this.cIndex = cIndex;
	   }
	   
	   public int getcIndex() {
		   return cIndex;
	   }
	}
	public interface DispManX extends Library {
		public DispManX INSTANCE = (DispManX)Native.loadLibrary("bcm_host", DispManX.class);
		
		public static final int DISPMANX_ID_MAIN_LCD = 0;
		public static final int DISPMANX_ID_AUX_LCD = 1;
		public static final int DISPMANX_ID_HDMI = 2;
		public static final int DISPMANX_ID_SDTV = 3;
		public static final int DISPMANX_ID_FORCE_LCD = 4;
		public static final int DISPMANX_ID_FORCE_TV = 5;
		public static final int DISPMANX_ID_FORCE_OTHER = 6;
		
		public static final int DISPMANX_PROTECTION_MAX =  0x0f;
		public static final int DISPMANX_PROTECTION_NONE = 0;
		public static final int DISPMANX_PROTECTION_HDCP = 11;
		
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
		displayInfo( DispManX.DISPMANX_ID_MAIN_LCD, "Main LCD" );
		displayInfo( DispManX.DISPMANX_ID_AUX_LCD, "AUX LCD" );
		displayInfo( DispManX.DISPMANX_ID_HDMI, "HDMI" );
		displayInfo( DispManX.DISPMANX_ID_SDTV, "SDTV" );
		displayInfo( DispManX.DISPMANX_ID_FORCE_LCD, "Force LCD" );
		displayInfo( DispManX.DISPMANX_ID_FORCE_TV, "Force TV" );
		displayInfo( DispManX.DISPMANX_ID_FORCE_OTHER, "Force other" );
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
	
	//sudo vi DispManXTest.java
	//sudo javac -cp lib/*:. DispManXTest.java
	//sudo java -cp lib/*:. DispManXTest 2 10 resourcesnew/favicon/apple-icon-144x144.png
	public static void main(String[] args) throws IOException, InterruptedException {
		DispManX dispMan = DispManX.INSTANCE;
		System.out.println("Initialized:" + dispMan.bcm_host_init());
        if ( args.length < 3 ) {
        	usage();
        	return;
        }
        
        int screen = Integer.parseInt( args[0] );
        int time = Integer.parseInt( args[1] );
        IntByReference pitch = new IntByReference();
        IntByReference width = new IntByReference();
        IntByReference height = new IntByReference();
        res( "get display size", dispMan.graphics_get_display_size( screen, width, height ) );
        System.out.printf( "display %d: %d x %d\n", screen, width.getValue(), height.getValue() );
        int display = dispMan.vc_dispmanx_display_open( screen );
        Memory bitmap = loadBitmapARGB8888( args[2], width, height, pitch );
        System.out.printf( "bitmap: %d x %d pitch->%d\n", width.getValue(), height.getValue(), pitch.getValue());

        IntByReference ref = new IntByReference();
        int resourceHandle = dispMan.vc_dispmanx_resource_create( VC_IMAGE_TYPE_T.VC_IMAGE_ARGB8888.getcIndex(), width.getValue(), height.getValue(), ref );
        
        VC_RECT_T.ByReference copyRect = new VC_RECT_T.ByReference();
        VC_RECT_T.ByReference sourceRect = new VC_RECT_T.ByReference();
        VC_RECT_T.ByReference destinationRect = new VC_RECT_T.ByReference();
        
        res( "rect set", dispMan.vc_dispmanx_rect_set( copyRect, 0, 0, width.getValue(), height.getValue() ) );
        //This seems to be some form of a zoom factor
        res( "rect set", dispMan.vc_dispmanx_rect_set( sourceRect, 0, 0, width.getValue()<<16, height.getValue()<<16 ) );
        res( "rect set", dispMan.vc_dispmanx_rect_set( destinationRect, 0, 0, width.getValue(), height.getValue() ) );

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
        		DispManX.DISPMANX_PROTECTION_NONE, 
        		alpha, 
        		0, 
        		VC_IMAGE_TRANSFORM_T.VC_IMAGE_ROT0.getcConst() );
        res( "submit", dispMan.vc_dispmanx_update_submit_sync( update ) );
        
        res( "resource write data", dispMan.vc_dispmanx_resource_write_data( 
        		resourceHandle, 
        		VC_IMAGE_TYPE_T.VC_IMAGE_ARGB8888.getcIndex(), 
        		pitch.getValue() , 
        		bitmap, 
        		destinationRect )
        	);

        Thread.sleep( time * 1000 );
        update = dispMan.vc_dispmanx_update_start( 0 );

        res( "element remove", dispMan.vc_dispmanx_element_remove( update, element ) );
        res( "submit", dispMan.vc_dispmanx_update_submit_sync( update ) );
        res( "resource delete", dispMan.vc_dispmanx_resource_delete( resourceHandle ) );
        res( "display close", dispMan.vc_dispmanx_display_close( display ) );
	}
}
