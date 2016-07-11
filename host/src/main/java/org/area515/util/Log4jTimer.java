package org.area515.util;

import org.apache.logging.log4j.ThreadContext;

public class Log4jTimer {
	public static long startTimer(String timerName) {
		long newTime = System.currentTimeMillis();
		ThreadContext.put(timerName, newTime + "");
		return newTime;
	}
	
	public static long splitTimer(String timerName) {
		long newTime = System.currentTimeMillis();
		long timeTaken = newTime - Long.parseLong(ThreadContext.get(timerName));
		ThreadContext.put(timerName, newTime + "");
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
}
