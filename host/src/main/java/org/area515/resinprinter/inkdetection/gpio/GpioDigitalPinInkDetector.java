package org.area515.resinprinter.inkdetection.gpio;

import java.io.IOException;

import org.area515.resinprinter.inkdetection.PrintMaterialDetector;
import org.area515.resinprinter.printer.Printer;
import org.area515.util.DynamicJSonSettings;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class GpioDigitalPinInkDetector implements PrintMaterialDetector, GpioPinListenerDigital {
	private GpioController gpio;
	private GpioPinDigitalInput inputPin;
	protected volatile float remainingResin;
	private boolean pinLowIsLow;
	
	@Override
	public void startMeasurement(Printer printer) {
		//Do nothing since we are event driven and don't have any CPU resources
	}

	@Override
	public float getPercentageOfPrintMaterialRemaining(Printer printer) throws IOException {
		return remainingResin;
	}

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		remainingResin = (pinLowIsLow && event.getState() == PinState.LOW) ||
				         (!pinLowIsLow && event.getState() == PinState.HIGH)?0:Float.MAX_VALUE;
	}

	@Override
	public void initializeDetector(DynamicJSonSettings settings) {
		Pin rPin = null;
		if (settings != null) {
			Object pin = settings.getSettings().get("Pin");
			if (pin == null) {
				rPin = RaspiPin.getPinByAddress(0);
			} else if (pin instanceof String) {
				rPin = RaspiPin.getPinByAddress(Integer.parseInt((String)pin));
			} else if (pin instanceof Number) {
				rPin = RaspiPin.getPinByAddress(((Number)pin).intValue());
			}
			Object pinFormat = settings.getSettings().get("PinLowIsLowInk");
			if (pinFormat instanceof String) {
				pinLowIsLow = Boolean.getBoolean((String)pinFormat);
			} else if (pinFormat instanceof Boolean) {
				pinLowIsLow = (Boolean)pinFormat;
			}
		} else {
			rPin = RaspiPin.getPinByAddress(0);
		}
		
		gpio = GpioFactory.getInstance();
		inputPin = gpio.provisionDigitalInputPin(rPin, "PrintMaterialDetector");
		gpio.setShutdownOptions(true, PinState.LOW, inputPin);
		inputPin.addListener(this);
	}
}
