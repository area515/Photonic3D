package org.area515.resinprinter.util.cron;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class RunCronPredictor {
	@Test
	public void main() {
		Date currentDate = new Date();
		CronPredictor cron = new CronPredictor(currentDate.getMinutes() + " * * * *", currentDate);
		System.out.println(currentDate + "->" + cron.nextMatchingDate());
		
		Calendar cal = Calendar.getInstance();
		/*cal.set(2014, 6, 15, 8, 8);
		cron = new CronPredictor("* * * 6 *", cal.getTime());
		System.out.println(cal.getTime() + "->" + cron.nextMatchingDate());
	
		cal.set(2014, 6, 15, 8, 8);
		cron = new CronPredictor("* * * 7 *", cal.getTime());
		System.out.println(cal.getTime() + "->" + cron.nextMatchingDate());
	
		cal.set(2014, 6, 15, 8, 8);
		cron = new CronPredictor("* * * 8 *", cal.getTime());
		System.out.println(cal.getTime() + "->" + cron.nextMatchingDate());*/
		
		cal.set(2014, 6, 15, 8, 8);
		cron = new CronPredictor("* 1 * * *", cal.getTime());
		System.out.println(cal.getTime() + "->" + cron.nextMatchingDate());
	}
}

