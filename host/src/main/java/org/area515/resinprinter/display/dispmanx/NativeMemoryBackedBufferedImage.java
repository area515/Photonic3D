package org.area515.resinprinter.display.dispmanx;

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
	
	public NativeMemoryBackedBufferedImage(Memory memory, ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied, Hashtable<?, ?> properties) {
		super(cm, raster, isRasterPremultiplied, properties);
		this.memory = memory;
	}
	
	public Memory getMemory() {
		return memory;
	}
	
    public int getPitch() {
        return ((4 * getWidth() + (getHeight())-1) & ~((getHeight())-1));
    }	    
    
    private static int getPitch(int x, int y) {
        return ((x + (y)-1) & ~((y)-1));
    }
	
	public static NativeMemoryBackedBufferedImage newInstance(int width, int height) {
		final int pitch = getPitch(4 * width, 32);
		Memory pixelMemory = new Memory(pitch * height);
		final ByteBuffer buffer = pixelMemory.getByteBuffer(0, pixelMemory.size());
		DataBuffer nativeScreenBuffer = new DataBuffer(DataBuffer.TYPE_INT, width * height) {
			@Override
			public int getElem(int bank, int i) {
				//TODO: COLORS ARE TOTALLY WACKED!
				//TODO: ALPHA IS BROKEN
				//TODO: (y*(pitch / bytesPerPixel) + x) * bytesPerPixel
				return (buffer.get(i * 4) << 24) | (buffer.get(i * 4 + 1) << 16) | (buffer.get(i * 4 + 2) << 8) | (buffer.get(i * 4 + 3));
			}
		  
			@Override
			public void setElem(int bank, int i, int val) {
				//TODO: COLORS ARE TOTALLY WACKED!
				//TODO: ALPHA IS BROKEN
				//TODO: (y*(pitch / bytesPerPixel) + x) * bytesPerPixel
				buffer.put(i * 4 + 0, (byte)((val | 0xFF000000) >> 24));
				buffer.put(i * 4 + 1, (byte)((val | 0xFF0000) >> 16));
				buffer.put(i * 4 + 2, (byte)((val | 0xFF00) >> 8));
				buffer.put(i * 4 + 3, (byte)(val | 0xFF));
			}
		};
	
		SampleModel argb = new SinglePixelPackedSampleModel(
		    DataBuffer.TYPE_INT, 
		    width, 
		    height,
		    new int[] { 0xFF0000, 0xFF00, 0xFF, 0xFF000000 });
		
		WritableRaster raster = new WritableRaster(argb, nativeScreenBuffer, new Point()){};
		
		return new NativeMemoryBackedBufferedImage(
				pixelMemory,
				new DirectColorModel(32, 0xFF0000, 0xFF00, 0xFF, 0xFF000000),
				raster, 
				false, 
				null);
	}
}
