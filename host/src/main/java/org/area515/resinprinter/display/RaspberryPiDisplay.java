package org.area515.resinprinter.display;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class RaspberryPiDisplay {
	public interface DispManX extends Library {
		DispManX INSTANCE = (DispManX)Native.loadLibrary("bcm_host", DispManX.class);
	}

	public RaspberryPiDisplay() {
		DispManX dispMan = DispManX.INSTANCE;
	}
}
