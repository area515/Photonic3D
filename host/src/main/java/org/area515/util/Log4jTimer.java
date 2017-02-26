package org.area515.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;

public class Log4jTimer {
	public static Map<String, String> GLOBAL = new HashMap<>();
	
	private static long startTimer(Map<String, String> map, String timerName) {
		long newTime = System.currentTimeMillis();
		map.put(timerName, newTime + "");
		return newTime;
	}
	
	public static long splitTimer(Map<String, String> map, String timerName) {
		long newTime = System.currentTimeMillis();
		long timeTaken = newTime - Long.parseLong(map.get(timerName));
		map.put(timerName, newTime + "");
		return timeTaken;
	}

	public static long completeTimer(Map<String, String> map, String timerName) {
		long newTime = System.currentTimeMillis();
		String value = map.get(timerName);
		if (value == null) {
			return -1;
		}
		
		long timeTaken = newTime - Long.parseLong(value);
		map.remove(timerName);
		return timeTaken;
	}

	public static long startTimer(String timerName) {
		return startTimer(ThreadContext.getContext(), timerName);
	}
	
	public static long startGlobalTimer(String timerName) {
		return startTimer(GLOBAL, timerName);
	}

	public static long splitTimer(String timerName) {
		return splitTimer(ThreadContext.getContext(), timerName);
	}

	public static long splitGlobalTimer(String timerName) {
		return splitTimer(GLOBAL, timerName);
	}
	
	public static long completeTimer(String timerName) {
		return completeTimer(ThreadContext.getContext(), timerName);
	}
	
	public static long completeGlobalTimer(String timerName) {
		return completeTimer(GLOBAL, timerName);
	}
}
