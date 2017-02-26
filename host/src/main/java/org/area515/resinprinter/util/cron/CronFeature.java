package org.area515.resinprinter.util.cron;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.plugin.Feature;
import org.area515.resinprinter.server.Main;

public class CronFeature implements Feature {
    private static final Logger logger = LogManager.getLogger();

    private List<CronTask> taskList = new ArrayList<CronTask>();
	
	public static class CronTask implements Callable {
		private String taskName;
		private Runnable runnable;
		private Callable callable;
		private CronPredictor predictor;
		private ScheduledFuture future;
		private boolean canceledTask;
		
		public CronTask(String taskName, Runnable runnable, SchedulingPattern pattern) {
			this.taskName = taskName;
			this.runnable = runnable;
			this.predictor = new CronPredictor(pattern);
		}
		
		public CronTask(String taskName, Callable callable, SchedulingPattern pattern) {
			this.taskName = taskName;
			this.callable = callable;
			this.predictor = new CronPredictor(pattern);
		}
		
		public synchronized void scheduleNextRun() throws RejectedExecutionException {
			if (canceledTask) {
				throw new RejectedExecutionException("Task cannot be executed again since it has been cancelled.");
			}
			future = Main.GLOBAL_EXECUTOR.schedule(this, predictor.nextMatchingTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}

		public synchronized boolean cancel() {
			canceledTask = true;
			return future.cancel(true);
		}
		
		@Override
		public Object call() throws Exception {
			Object returnObject;
			try {
				if (runnable != null) {
					runnable.run();
					returnObject = Void.TYPE;
				} else if (callable != null){
					returnObject = callable.call();
				} else {
					throw new RejectedExecutionException("Must implement either a runnable or a callable.");
				}
				scheduleNextRun();
				return returnObject;
			} catch (RejectedExecutionException e) {
			    logger.error("I will NEVER attempt to execute this task again:" + taskName, e);
			    throw e;
			} catch (Exception e) {
			    logger.error("I will attempt to execute this task again:" + taskName, e);
			    throw e;
			} catch (Throwable t) {
			    logger.error("Can't recover, I will NEVER attempt to execute this task again:" + taskName, t);
			    throw t;
			}
		}
	}
	
	@Override
	public void start(URI uri) throws Exception {
		
	}

	@Override
	public void stop() {
		
	}
}
