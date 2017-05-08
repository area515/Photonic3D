package org.area515.resinprinter.plugin;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.security.FriendshipFeature;
import org.area515.resinprinter.security.UserManagementFeature;
import org.area515.resinprinter.server.HostProperties;

public class FeatureManager {
	private static final Logger logger = LogManager.getLogger();
	private static Map<Feature, String> features = null;
	
	private static void initFeatures() {
		features = new HashMap<Feature, String>();
		Map<Class<Feature>, String> featureClasses = HostProperties.Instance().getFeatures();
		for (Entry<Class<Feature>, String> currentEntry : featureClasses.entrySet()) {
			try {
				features.put(currentEntry.getKey().newInstance(), currentEntry.getValue());
			} catch (Exception e) {
				logger.error("Couldn't create feature", e);
			}
		}

	}
	
	public static void start(URI uri) {
		if (features == null) {
			initFeatures();
		}
		
		for (Entry<Feature, String> currentFeature : features.entrySet()) {
			try {
				currentFeature.getKey().start(uri, currentFeature.getValue());
			} catch (Exception e) {
				logger.error("Couldn't start feature", e);
			}
		}
	}
	
	public static void shutdown() {
		for (Feature currentFeature : features.keySet()) {
			currentFeature.stop();
		}
	}
	
	public static Map<String, FriendshipFeature> getFriendshipFeatures() {
		if (features == null) {
			initFeatures();
		}
		
		Map<String, FriendshipFeature> friendshipFeatures = new HashMap<String, FriendshipFeature>();
		for (Feature currentFeature : features.keySet()) {
			if (currentFeature instanceof FriendshipFeature) {
				friendshipFeatures.put(((FriendshipFeature) currentFeature).getName(), (FriendshipFeature)currentFeature);
			}
		}
		
		return friendshipFeatures;
	}
	
	public static UserManagementFeature getUserManagementFeature() {
		if (features == null) {
			initFeatures();
		}
		
		for (Feature currentFeature : features.keySet()) {
			if (currentFeature instanceof UserManagementFeature) {
				return (UserManagementFeature)currentFeature;
			}
		}
		
		return null;
	}
}
