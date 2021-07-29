package com.wgilster.dispmanx.window;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import com.sun.jna.Memory;

public class NativeMemoryBackedBufferedImage extends BufferedImage {
	private Memory memory;
	
	public static class ByteDataBuffer extends DataBuffer {
		private int pitch;
		private int width;
		private int bytesPerPixel;
		private ByteBuffer buffer;
		
		protected ByteDataBuffer(int dataType, int width, int height, int pitch, int bytesPerPixel, Memory pixelMemory) {
			super(dataType, width * height);
			this.pitch = pitch;
			this.width = width;
			this.bytesPerPixel = bytesPerPixel;
			this.buffer = pixelMemory.getByteBuffer(0, pixelMemory.size());
		}

		public byte[] getData() {
			return buffer.array();
		}
		
		@Override
		public int getElem(int bank, int i) {
			int y = (i / width);
			int x = (i % width);
			int index = y*(pitch * bytesPerPixel) + x * bytesPerPixel;
			
			return (buffer.get(index) & 0xFF) | ((buffer.get(index + 1) & 0xFF) << 8) | ((buffer.get(index + 2) & 0xFF) << 16) | ((buffer.get(index + 3) & 0xFF) << 24);
		}
		
		@Override
		public void setElem(int bank, int i, int val) {
			int y = (i / width);
			int x = (i % width);
			int index = y*(pitch * bytesPerPixel) + x * bytesPerPixel;

				buffer.put(index + 0, (byte)(val));				//b
				buffer.put(index + 1, (byte)((val >> 8)));		//g
				buffer.put(index + 2, (byte)((val >> 16)));		//r
				buffer.put(index + 3, (byte)((val >> 24)));		//a
			
			//Red dot debugging
			/*y = (redIndex / width);
			x = (redIndex % width);
			int red = y*(pitch * bytesPerPixel) + x * bytesPerPixel;
			buffer.put(red + 0, (byte)0);				//b
			buffer.put(red + 1, (byte)0);		//g
			buffer.put(red + 2, (byte)0xFF);		//r
			buffer.put(red + 3, (byte)0xFF);		//a*/
			
			
			
			/*if (i == redIndex) {
        		System.out.println("X:" + x + " Y:" + y + " i:" + i + " index:" + (index));
				System.out.println("val:" + Integer.toBinaryString((byte)val)); //1 0 1 0 = g | 1 1 0 0 = r | 1 0 0 1 = b
				if (breakForException) {
					try {
						throw new RuntimeException("Inspect");
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
				}
			}*/
		}

		
	}
	
	public NativeMemoryBackedBufferedImage(Memory memory, ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied, Hashtable<?, ?> properties) {
		super(cm, raster, isRasterPremultiplied, properties);
		this.memory = memory;
	}
	
	public Memory getMemory() {
		return memory;
	}

	//Becareful modifying this method, because Display.createResource() uses it.
	public int getPitch() {
        return ((getWidth() + (16)-1) & ~((16)-1)) * 4;
    }
    
    private static int getPitch(int x, int y) {
        return ((x + (y)-1) & ~((y)-1));
    }
    
    public void clear() {
    	memory.clear();
    }
    
	public static NativeMemoryBackedBufferedImage newInstance(int width, int height, boolean initialize) {
		int bytesPerPixel = 4;
		int pitch = getPitch(width, 16) * bytesPerPixel;
		Memory pixelMemory = new Memory(pitch * height);
		if (initialize) {
			pixelMemory.clear();
		}
		return newInstance(width, height, bytesPerPixel, pixelMemory);
	}
	private static String binaryString(byte b) {
		return Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
	}
	
	private static boolean isPowerOfTwo(int x) {
	    return (x != 0) && ((x & (x - 1)) == 0);
	}
	public static void main(String[] args) {
		int width = 19;
		int bytesPerPixel = 4;
		/*int pitch = getPitch(width, 32);
		System.out.println(pitch);
		for (int t = 0; t < 1000; t++) {
			int widthTotal = width;
			int index = (t / pitch) * widthTotal + (t % pitch);
			System.out.println(t + "=" + index);
			//int index = (y*(pitch / bytesPerPixel) + x) * bytesPerPixel;
		}*/
		int pitch = getPitch( width, 16 ) * bytesPerPixel;
		System.out.println("Pitch:" + pitch);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < width; x++) {
        		System.out.println("X:" + x + "Y:" + y + " i:" + (y * width + x) + "="+ (y*(pitch / bytesPerPixel) + x) * bytesPerPixel);
        		
            }
        }
	}
	
	private static int redIndex = 0;
	private static boolean breakForException = false;
	public static void incrementRed() {
		redIndex += 1;
	}
	public static void decrementRed() {
		redIndex -= 1;
	}
	public static void toggleBreakForException() {
		breakForException = !breakForException;
	}
	
	public static NativeMemoryBackedBufferedImage newInstance(int width, int height, int bytesPerPixel, Memory pixelMemory) {
		/*if (!isPowerOfTwo(width)) {
			throw new RuntimeException("Width must be an even power of two or the pitch get's 'off'");
		}*/
		int pitch = getPitch(width, 16);
/*System.out.println("=======");
System.out.println("pitch:" + pitch);
System.out.println("width:" + width);
System.out.println("height:" + height);
System.out.println("size:" + pixelMemory.size());*/
		ByteDataBuffer nativeScreenBuffer = new ByteDataBuffer(DataBuffer.TYPE_INT, width, height, pitch, bytesPerPixel, pixelMemory);
	
		SampleModel argb = new SinglePixelPackedSampleModel(
		    DataBuffer.TYPE_INT, 
		    width, 
		    height,
		    new int[] { 0xFF0000, 0xFF00, 0xFF, 0xFF000000 });
		WritableRaster raster = new WritableRaster(argb, nativeScreenBuffer, new Point(0, 0)) {};
		return new NativeMemoryBackedBufferedImage(
				pixelMemory,
				new DirectColorModel(32, 0xFF0000, 0xFF00, 0xFF, 0xFF000000),
				raster, 
				false, 
				null);
	}
	
	/*private Memory loadBitmapRGB565(BufferedImage image, Memory destPixels, IntByReference width, IntByReference height, IntByReference pitchByRef) {
		int bytesPerPixel = 2;
		int pitch = getPitch(bytesPerPixel * image.getWidth(), 32);
		pitchByRef.setValue(pitch);
		if (destPixels == null) {
			destPixels = new Memory(pitch * image.getHeight());
		}
		
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

	private Memory loadBitmapARGB8888(BufferedImage image, Memory destPixels, IntByReference width, IntByReference height, IntByReference pitchByRef) {
		int bytesPerPixel = 4;
		int pitch = getPitch(bytesPerPixel * image.getWidth(), 32);
		pitchByRef.setValue(pitch);
		if (destPixels == null) {
			destPixels = new Memory(pitch * image.getHeight());
		}
		
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
        		destPixels.setInt((y*(pitch / bytesPerPixel) + x) * bytesPerPixel, image.getRGB(x, y));
            }
        }

        width.setValue(image.getWidth());
        height.setValue(image.getHeight());
        return destPixels;
	}*/

}