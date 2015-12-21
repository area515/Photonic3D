package org.area515.resinprinter.discover;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.area515.resinprinter.server.HostProperties;

public class BroadcastManager {
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
				System.out.println("Couldn't start advertiser");
				e.printStackTrace();
			}
		}
	}
	
	public static void shutdown() {
		for (Advertiser currentAdvertiser : advertisers) {
			currentAdvertiser.stop();
		}
	}
}
