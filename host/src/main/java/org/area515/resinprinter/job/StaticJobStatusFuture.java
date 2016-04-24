package org.area515.resinprinter.job;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StaticJobStatusFuture implements Future<JobStatus> {
	private JobStatus status;
	
	public StaticJobStatusFuture(JobStatus status) {
		this.status = status;
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public JobStatus get() throws InterruptedException, ExecutionException {
		return status;
	}

	@Override
	public JobStatus get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return status;
	}
}
