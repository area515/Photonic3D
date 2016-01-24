package org.area515.resinprinter.discover;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.server.HostProperties;

public class BroadcastManager {
	private static final Logger logger = LogManager.getLogger();
	private static List<Advertiser> advertisers = null;
	
	public static void start(URI uri) {
		if (advertisers != null) {
			return;
		}
		
		advertisers = new ArrayList<Advertiser>();
		List<Class<Advertiser>> advertiserClasses = HostProperties.Instance().getAdvertisers();
		for (Class<Advertiser> currentClass : advertiserClasses) {
			Advertiser advertiser;
			try {
				advertiser = currentClass.newInstance();
				advertiser.start(uri);
			} catch (Exception e) {
				logger.error("Couldn't start advertiser", e);
			}
		}
	}
	
	public static void shutdown() {
		for (Advertiser currentAdvertiser : advertisers) {
			currentAdvertiser.stop();
		}
	}
}
