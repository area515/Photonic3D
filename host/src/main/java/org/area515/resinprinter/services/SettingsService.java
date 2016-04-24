package org.area515.resinprinter.services;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.area515.resinprinter.api.SwaggerStrings;
import org.area515.resinprinter.server.CwhEmailSettings;
import org.area515.resinprinter.server.HostInformation;
import org.area515.resinprinter.server.HostProperties;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value="settings", description="This service allows a client to manage all of the global settings in Photonic 3D. "
		+ "This service is just a light wrapper for HostProperties that are defined in config.properties.")
@Path("settings")
public class SettingsService {
	public static SettingsService INSTANCE = new SettingsService();
	
	private SettingsService(){}
	
    @ApiOperation(value="This method returns a hint towards the visible cards that should be shown in the GUI. The config.properties key for this is: visibleCards")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerStrings.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerStrings.UNEXPECTED_ERROR)})
	//TODO: This should be limited by user permissions...
	@GET
	@Path("visibleCards")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getVisibleCards() {
		return HostProperties.Instance().getVisibleCards();
	}
	
    @ApiOperation(value="Returns the integer based version number of Photonic3D found in the build.number file.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerStrings.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerStrings.UNEXPECTED_ERROR)})
	@GET
	@Path("integerVersion")
	@Produces(MediaType.APPLICATION_JSON)
	public int getIntegerVersion() {
		return HostProperties.Instance().getVersionNumber();
	}
	
    @ApiOperation(value="Get the email settings that are setup for the NotificationManager and taking diagnostics.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerStrings.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerStrings.UNEXPECTED_ERROR)})
	@GET
	@Path("emailSettings")
	@Produces(MediaType.APPLICATION_JSON)
	public CwhEmailSettings getEmailSettings() {
		return HostProperties.Instance().loadEmailSettings();
	}
    
    @ApiOperation(value="Allows the email settings to be setup for the NotificationManager and taking diagnostics.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerStrings.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerStrings.UNEXPECTED_ERROR)})
	@PUT
	@Path("emailSettings")
	@Consumes(MediaType.APPLICATION_JSON)
	public void setEmailSettings(CwhEmailSettings settings) {
		HostProperties.Instance().saveEmailSettings(settings);
	}
	
    @ApiOperation(value="This will return the host information(device name and manufacturer) that is setup in config.properties. "
    		+ "This host information is also what identifies Photonic 3D printers through multicast to DLNA and UPNP clients on the user's network.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerStrings.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerStrings.UNEXPECTED_ERROR)})
	@GET
	@Path("hostInformation")
	@Produces(MediaType.APPLICATION_JSON)
	public HostInformation getHostInformation() {
		return HostProperties.Instance().loadHostInformation();
	}

    @ApiOperation(value="This will set the host information(device name and manufacturer) that is configured in config.properties. "
    		+ "This host information is also what identifies Photonic 3D printers through multicast to DLNA and UPNP clients on the user's network.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SwaggerStrings.SUCCESS),
            @ApiResponse(code = 500, message = SwaggerStrings.UNEXPECTED_ERROR)})
    @PUT
	@Path("hostInformation")
	public void setHostInformation(HostInformation info) {
		HostProperties.Instance().saveHostInformation(info);
	}
}