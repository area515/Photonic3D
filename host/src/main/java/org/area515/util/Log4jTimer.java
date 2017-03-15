package org.area515.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;

public class Log4jTimer {
	public static Map<String, String> GLOBAL = new HashMap<>();

	public static long startTimer(String timerName) {
		long newTime = System.currentTimeMillis();
		ThreadContext.put(timerName, newTime + "");
		return newTime;
	}
	
	public static long startGlobalTimer(String timerName) {
		long newTime = System.currentTimeMillis();
		GLOBAL.put(timerName, newTime + "");
		return newTime;
	}

	public static long splitTimer(String timerName) {
		long newTime = System.currentTimeMillis();
		String value = ThreadContext.get(timerName);
		if (value == null) {
			return startTimer(timerName);
		}

		long timeTaken = newTime - Long.parseLong(value);
		ThreadContext.put(timerName, newTime + "");
		return timeTaken;
	}

	public static long splitGlobalTimer(String timerName) {
		long newTime = System.currentTimeMillis();		
		String value = GLOBAL.get(timerName);
		if (value == null) {
			return startGlobalTimer(timerName);
		}

		long timeTaken = newTime - Long.parseLong(value);
		GLOBAL.put(timerName, newTime + "");
		return timeTaken;
	}
	
	public static long completeTimer(String timerName) {
		long newTime = System.currentTimeMillis();
		String value = ThreadContext.get(timerName);
		if (value == null) {
			return -1;
		}
		
		long timeTaken = newTime - Long.parseLong(value);
		ThreadContext.remove(timerName);
		return timeTaken;
	}
	
	public static long completeGlobalTimer(String timerName) {
		long newTime = System.currentTimeMillis();
		String value = GLOBAL.get(timerName);
		if (value == null) {
			return -1;
		}
		
		long timeTaken = newTime - Long.parseLong(value);
		GLOBAL.remove(timerName);
		return timeTaken;
	}
}
