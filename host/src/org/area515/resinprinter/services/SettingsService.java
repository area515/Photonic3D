package org.area515.resinprinter.services;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.area515.resinprinter.server.HostProperties;

@Path("settings")
public class SettingsService {
	public static SettingsService INSTANCE = new SettingsService();
	
	private SettingsService(){}
	
	//TODO: This should be limited by user permissions...
	@GET
	@Path("visibleCards")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getVisibleCards() {
		return HostProperties.Instance().getVisibleCards();
	}
	
	@GET
	@Path("integerVersion")
	@Produces(MediaType.APPLICATION_JSON)
	public int getIntegerVersion() {
		return HostProperties.Instance().getVersionNumber();
	}
}