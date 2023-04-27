package org.area515.resinprinter.discover;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.client.Main;
import org.area515.resinprinter.plugin.Feature;
import org.area515.resinprinter.server.HostInformation;
import org.area515.resinprinter.server.HostProperties;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.resource.Resource;
import org.fourthline.cling.model.types.DLNADoc;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.connectionmanager.AbstractPeeringConnectionManagerService;

//import com.hp.jipp.encoding.IppPacket;

public class UPNPAdvertiser implements Feature {
	private static final Logger logger = LogManager.getLogger();
	public static UPNPSetup INSTANCE;
	
	private UpnpServiceImpl upnpService;
	//(ip.addr eq 192.168.0.88 and ip.addr eq 239.255.255.250) and (udp.port eq 1900 or tcp.port eq 631)
	public static class UPNPSetup {
		private String deviceName = null;
		private String manufacturer = null;
		private String deviceReleaseString = HostProperties.Instance().getReleaseTagName();
		private int deviceVersion = HostProperties.Instance().getVersionNumber();
		private String deviceType = Main.PRINTER_TYPE;
		private int upnpStreamPort = Main.UPNP_STREAM_PORT;
		
		public UPNPSetup(HostInformation info) {
			this.deviceName = info.getDeviceName();
			this.manufacturer = info.getManufacturer();
		}
		
		public String getDeviceName() {
			return deviceName;
		}
		public String getManufacturer() {
			return manufacturer;
		}
	}
	
	public UPNPSetup getSetup() {
		if (INSTANCE == null) {
			INSTANCE = new UPNPSetup(HostProperties.Instance().loadHostInformation());
		}
		
		return INSTANCE;
	}
	
	@Override
	public void start(URI webPresentationURI, String settings) {
		try {
			UDN udn = UDN.uniqueSystemIdentifier(getSetup().deviceName + getSetup().deviceReleaseString + getSetup().manufacturer);
			//DeviceType type = new UDADeviceType(getSetup().deviceType, getSetup().deviceVersion);
			DeviceType type = new DeviceType("NotUsed", "NotUsed") {
				public String getType() {
					return getSetup().deviceType;
				}
				public String toString() {
					return getSetup().deviceType;
				}
			};//*/
			DeviceDetails details = new DeviceDetails(getSetup().deviceName,
					new ManufacturerDetails(getSetup().manufacturer), 
						new ModelDetails(
								getSetup().deviceName, 
								getSetup().deviceName, 
								"v" + getSetup().deviceReleaseString), 
							webPresentationURI, 
					new DLNADoc[] {
							new DLNADoc("DMS", DLNADoc.Version.V1_5),
							new DLNADoc("M-DMS", DLNADoc.Version.V1_5) },
					null);
			
			List<Icon> icons = new ArrayList<Icon>();
			File iconFiles[] = new File(HostProperties.Instance().getFirstActiveSkin().getResourceBase(), "favicon").listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					name = name.toLowerCase();
					if (!name.endsWith(".jpg") && !name.endsWith(".gif") && !name.endsWith(".png")) {
						return false;
					}
					
					return name.contains("48") || name.contains("120");
				}
			});
			
			if (iconFiles != null) {
				for (File currentFile : iconFiles) {
					String mimeType = Files.probeContentType(currentFile.toPath());
					BufferedImage image = ImageIO.read(currentFile);
					icons.add(new Icon(mimeType, image.getWidth(), image.getHeight(), image.getColorModel().getPixelSize(), new URI(webPresentationURI.toString() + "/favicon/" + currentFile.getName())));
				}
			}
			
			LocalService<PrinterDirectoryService> dlnaContentManagerService = new AnnotationLocalServiceBinder().read(PrinterDirectoryService.class);
			PrinterHostSettingsServiceManager manager = new PrinterHostSettingsServiceManager<PrinterDirectoryService>(getSetup(), webPresentationURI, dlnaContentManagerService, PrinterDirectoryService.class);
			dlnaContentManagerService.setManager(manager);//new DefaultServiceManager<PrinterDirectoryService>(contentManagerService, PrinterDirectoryService.class));
			
			LocalService<AbstractPeeringConnectionManagerService> connectionManagerService = new AnnotationLocalServiceBinder().read(AbstractPeeringConnectionManagerService.class);
			connectionManagerService.setManager(new DefaultServiceManager<AbstractPeeringConnectionManagerService>(connectionManagerService, AbstractPeeringConnectionManagerService.class));
				
			PhotonicDevice printerDevice = new PhotonicDevice(
					new DeviceIdentity(udn), 
					type, 
					details, 
					icons.toArray(new Icon[icons.size()]),
					new LocalService[]{dlnaContentManagerService, connectionManagerService});//, ippContentManagerService});
			
			PhotonicUpnpServiceConfiguration serviceConfiguration = new PhotonicUpnpServiceConfiguration(getSetup().upnpStreamPort);
			UpnpService upnpService = new UpnpServiceImpl(serviceConfiguration);/* {
			//All of this was designed to allow our prints to be detected as 3dPrinters... Turning off for now...
			    protected ProtocolFactory createProtocolFactory() {
			        return new ProtocolFactoryImpl(this) {
			            public SendingNotificationAlive createSendingNotificationAlive(LocalDevice localDevice) {
			                return new SendingNotificationAlive(getUpnpService(), localDevice) {
			                    public void sendMessages(Location descriptorLocation) throws RouterException {
			                        logger.debug("Sending root device messages: " + getDevice());
			                        List<OutgoingNotificationRequest> rootDeviceMsgs =
			                                createDeviceMessages(getDevice(), descriptorLocation);
			                        for (OutgoingNotificationRequest upnpMessage : rootDeviceMsgs) {
			                            getUpnpService().getRouter().send(upnpMessage);
			                        }
			                        //OutgoingNotificationRequestDeviceType.class ^^^
			                        if (getDevice().hasEmbeddedDevices()) {
			                            for (LocalDevice embeddedDevice : getDevice().findEmbeddedDevices()) {
			                            	logger.debug("Sending embedded device messages: " + embeddedDevice);
			                                List<OutgoingNotificationRequest> embeddedDeviceMsgs =
			                                        createDeviceMessages(embeddedDevice, descriptorLocation);
			                                for (OutgoingNotificationRequest upnpMessage : embeddedDeviceMsgs) {
			                                    getUpnpService().getRouter().send(upnpMessage);
			                                }
			                            }
			                        }
			                        
			                        List<OutgoingNotificationRequest> serviceTypeMsgs =
			                                createServiceTypeMessages(getDevice(), descriptorLocation);
			                        if (serviceTypeMsgs.size() > 0) {
			                        	logger.debug("Sending service type messages");
			                            for (OutgoingNotificationRequest upnpMessage : serviceTypeMsgs) {
			                                getUpnpService().getRouter().send(upnpMessage);
			                            }
			                        }
			                    }

			                    protected void execute() throws RouterException {
			                        List<NetworkAddress> activeStreamServers =
			                            getUpnpService().getRouter().getActiveStreamServers(null);
			                        if (activeStreamServers.size() == 0) {
			                        	logger.error("Aborting notifications, no active stream servers found (network disabled?)");
			                            return;
			                        }

			                        // Prepare it once, it's the same for each repetition
			                        List<Location> descriptorLocations = new ArrayList();
			                        for (NetworkAddress activeStreamServer : activeStreamServers) {
			                            descriptorLocations.add(
			                                    new Location(
			                                            activeStreamServer,
			                                            getUpnpService().getConfiguration().getNamespace().getDescriptorPathString(getDevice())
			                                    ));
			                            descriptorLocations.add(
			                                    new Location(
			                                            new NetworkAddress(activeStreamServer.getAddress(), 631),
			                                            getUpnpService().getConfiguration().getNamespace().getDescriptorPathString(getDevice())
			                                    ) {
			                                        public URL getURL() {
			                                        	try {
															return new URL("ipp", url.getHost(), 631, "/ipp/print", new URLStreamHandler() {
																@Override
																protected URLConnection openConnection(URL u) throws IOException {
																	throw new IllegalArgumentException("This is just as a template and no one should call this!!!");
																}
															});
			                                            } catch (Exception ex) {
			                                                throw new IllegalArgumentException("Address, port, and URI can not be converted to URL", ex);
			                                            }
			                                        }
			                                    }
			                            );
			                        }
			                        
			                        for (int i = 0; i < getBulkRepeat(); i++) {
			                            try {

			                                for (Location descriptorLocation : descriptorLocations) {
			                                    sendMessages(descriptorLocation);
			                                }

			                                // UDA 1.0 is silent about this but UDA 1.1 recomments "a few hundred milliseconds"
			                                logger.debug("Sleeping " + getBulkIntervalMilliseconds() + " milliseconds");
			                                Thread.sleep(getBulkIntervalMilliseconds());

			                            } catch (InterruptedException ex) {
			                                logger.warn("Advertisement thread was interrupted: " + ex);
			                            }
			                        }
			                    }

			                };
			            }
			        };
			    }
			};*/

			
			
			if (!upnpService.getRouter().isEnabled()) {
				throw new IllegalArgumentException("It doesn't seem as though a network is available to publish this server, or an advertiser has already been started on this address.");
			}
			
			// Broadcast a search message for all devices
			ControlPoint controlPoint = upnpService.getControlPoint();
			controlPoint.search(new STAllHeader());
			
			// Add the bound local device to the registry
			upnpService.getRegistry().addDevice(printerDevice);
			
			//NetworkAddress hooked = upnpService.getRouter().getActiveStreamServers(upnpService.getRouter().getConfiguration().createNetworkAddressFactory().getBindAddresses().next()).get(0);
			
			//upnpService.getRouter().getConfiguration().createNetworkAddressFactory().getBindAddresses()
			//By this point the externallyAccessableIP will be setup with the local IP if it started off null
			for (Resource device : upnpService.getRegistry().getResources()) {
				if (device.getModel() instanceof LocalDevice) {
					logger.info("===========");
					logger.info("{} exposed on: {}", device.getModel(), device.getPathQuery());
					logger.info("Relative UPNP root descriptor: {}", device.getPathQuery());
					logger.info("===========");
				} else {
					logger.info("{} exposed on: {}", device.getModel(), device.getPathQuery());
				}
			}
			
			//IppPacket d;
		} catch (Exception e) {
			logger.error("Couldn't advertise URI:" + webPresentationURI, e);
			throw new RuntimeException("Couldn't advertise URI:" + webPresentationURI);
		}
	}

	@Override
	public void stop() {
		upnpService.shutdown();
	}
}
