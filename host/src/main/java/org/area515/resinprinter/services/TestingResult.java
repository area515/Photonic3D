package org.area515.resinprinter.services;


public class TestingResult {
	private boolean error;
	private String errorDescription;
	private int errorLineNumber;
	private Object result;
	
	public static class Chart {
		private double[][] data;
		private String[] labels;
		private String[] series;
		private String name;
		
		public Chart(double[][] data, String[] labels, String[] series, String name) {
			this.data = data;
			this.labels = labels;
			this.series = series;
			this.name = name;
		}
		
		public double[][] getData() {
			return data;
		}

		public String[] getLabels() {
			return labels;
		}

		public String[] getSeries() {
			return series;
		}
		
		public String getName() {
			return name;
		}
	}
	
	public static class ChartData {
		private String name;
		private double start;
		private double stop;
		private double increment;
		
		public ChartData(double start, double stop, double increment, String name) {
			this.start = start;
			this.stop = stop;
			this.increment = increment;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public double getStart() {
			return start;
		}

		public double getStop() {
			return stop;
		}

		public double getIncrement() {
			return increment;
		}
		
		public int getSize() {
			return (int)((stop - start) / increment);
		}
	}
	
	private TestingResult() {}
	
	public TestingResult(String errorDescription, int errorLineNumber) {
		this.error = true;
		this.errorDescription = errorDescription;
		this.errorLineNumber = errorLineNumber;
	}
	
	public TestingResult(Object result) {
		this.error = false;
		this.result = result;
	}

	
	public boolean isError() {
		return error;
	}

	public String getErrorDescription() {
		return errorDescription;
	}

	public int getErrorLineNumber() {
		return errorLineNumber;
	}

	public Object getResult() {
		return result;
	}
}
