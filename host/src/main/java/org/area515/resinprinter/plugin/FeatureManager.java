package org.area515.resinprinter.plugin;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.server.HostProperties;

public class FeatureManager {
	private static final Logger logger = LogManager.getLogger();
	private static List<Feature> features = null;
	
	public static void start(URI uri) {
		if (features != null) {
			return;
		}
		
		features = new ArrayList<Feature>();
		List<Class<Feature>> featureClasses = HostProperties.Instance().getFeatures();
		for (Class<Feature> currentClass : featureClasses) {
			Feature feature;
			try {
				feature = currentClass.newInstance();
				feature.start(uri);
			} catch (Exception e) {
				logger.error("Couldn't start feature", e);
			}
		}
	}
	
	public static void shutdown() {
		for (Feature currentFeature : features) {
			currentFeature.stop();
		}
	}
}
