package org.area515.resinprinter.plugin;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.security.UserManagementFeature;
import org.area515.resinprinter.server.HostProperties;

public class FeatureManager {
	private static final Logger logger = LogManager.getLogger();
	private static List<Feature> features = null;
	
	private static void initFeatures() {
		features = new ArrayList<Feature>();
		List<Class<Feature>> featureClasses = HostProperties.Instance().getFeatures();
		for (Class<Feature> currentClass : featureClasses) {
			try {
				features.add(currentClass.newInstance());
			} catch (Exception e) {
				logger.error("Couldn't create feature", e);
			}
		}

	}
	
	public static void start(URI uri) {
		if (features == null) {
			initFeatures();
		}
		
		for (Feature currentFeature : features) {
			try {
				currentFeature.start(uri);
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
	
	public static UserManagementFeature getUserManagementFeature() {
		if (features == null) {
			initFeatures();
		}
		
		for (Feature currentFeature : features) {
			if (currentFeature instanceof UserManagementFeature) {
				return (UserManagementFeature)currentFeature;
			}
		}
		
		return null;
	}
}
