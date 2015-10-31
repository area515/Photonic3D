package org.area515.resinprinter.services;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.area515.resinprinter.server.CwhEmailSettings;
import org.area515.resinprinter.server.HostInformation;
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
	
	@GET
	@Path("emailSettings")
	@Produces(MediaType.APPLICATION_JSON)
	public CwhEmailSettings getEmailSettings() {
		return HostProperties.Instance().loadEmailSettings();
	}
	@PUT
	@Path("emailSettings")
	@Consumes(MediaType.APPLICATION_JSON)
	public void setEmailSettings(CwhEmailSettings settings) {
		HostProperties.Instance().saveEmailSettings(settings);
	}
	
	@GET
	@Path("hostInformation")
	@Produces(MediaType.APPLICATION_JSON)
	public HostInformation getHostInformation() {
		return HostProperties.Instance().loadHostInformation();
	}
	@PUT
	@Path("hostInformation")
	public void setHostInformation(HostInformation info) {
		HostProperties.Instance().saveHostInformation(info);
	}
}