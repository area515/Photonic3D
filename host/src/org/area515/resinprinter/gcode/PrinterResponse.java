package org.area515.resinprinter.gcode;

import java.util.regex.Matcher;

public class PrinterResponse {
	private Matcher lastLine;
	private StringBuilder fullResponse;
	
	public Matcher getLastLineMatcher() {
		return lastLine;
	}
	public void setLastLineMatcher(Matcher lastLine) {
		this.lastLine = lastLine;
	}
	
	public StringBuilder getFullResponse() {
		return fullResponse;
	}
	public void setFullResponse(StringBuilder fullResponse) {
		this.fullResponse = fullResponse;
	}
}
