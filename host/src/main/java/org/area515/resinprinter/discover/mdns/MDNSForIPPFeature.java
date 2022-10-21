package org.area515.resinprinter.discover.mdns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.UUID;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.job.PrintFileProcessor;
import org.area515.resinprinter.plugin.Feature;
import org.area515.resinprinter.server.HostInformation;
import org.area515.resinprinter.server.HostProperties;

public class MDNSForIPPFeature implements Feature {
    private static final Logger logger = LogManager.getLogger();
	private JmDNS jmdns = null;
	
	@Override
	public void start(URI uri, String settings) throws Exception {
	     try {
	            // Create a JmDNS instance
	            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
	            
	            HostInformation info = HostProperties.Instance().loadHostInformation();
	            HashMap<String,String> properties = new HashMap<String, String>();
	            properties.put("adminurl", uri + "");
	            properties.put("note", "This is a note");
	            
	            StringBuilder printerFilesSupported = new StringBuilder();
	            for (PrintFileProcessor processor : HostProperties.Instance().getPrintFileProcessors()) {
	            	for (String fileExtension : processor.getFileExtensions()) {
	            		printerFilesSupported.append(",");
	            		printerFilesSupported.append(fileExtension);
	            	}
	            }
	            properties.put("pdl", printerFilesSupported.toString().substring(1));
	            properties.put("rp", "ipp/print");
	            properties.put("ty", info.getDeviceName() + " " + info.getManufacturer());
	            properties.put("UUID", UUID.randomUUID() + "");
	            
	            /*properties.put("UUID", "cfe92100-67c4-11d4-a45f-64eb8c13be5c");
	            properties.put("note", "Backroot");
	            properties.put("product", "Pepsid");
	            properties.put("usb_MFG", "MFG");
	            properties.put("Duplex", "T");
	            properties.put("Fax", "T");
	            properties.put("adminurl", "http://" + uri.getHost() + ":80/PRESENTATION/BONJOUR");
	            properties.put("PaperMax", "legal-A4");
	            properties.put("kind", "document,envelope,photo");
	            properties.put("usb_MDL", "XP-820 Series");
	            properties.put("rp", "ipp/print");
	            properties.put("rfo", "ipp/faxout");
	            properties.put("URF", "CP1,MT1-3-8-10-11-12,PQ4-5,OB9,OFU0,RS360,SRGB24,W8,DM3,IS1-7-18,V1.4");
	            properties.put("TLS", "1.2");
	            properties.put("print_wfds", "T");
	            properties.put("txtvers", "T");
	            properties.put("pdl", "application/octet-stream,image/pwg-raster,image/urf,image/jpeg");
	            properties.put("Scan", "T");
	            properties.put("qtotal", "1");
	            properties.put("ty", "Pepsum");
	            properties.put("Color", "T");*/
	            
	            // Register a service
	            ServiceInfo serviceInfo = ServiceInfo.create("_ipps-3d", "Photonic3DPrintService", 631, 0, 0, properties);
	            jmdns.registerService(serviceInfo);
	            serviceInfo = ServiceInfo.create("_ipps", "Photonic2DPrintService", 631, 0, 0, properties);
	            jmdns.registerService(serviceInfo);
	            logger.info("IPP Printer advertised:");
	        } catch (IOException e) {
	            logger.error("IPP Printer failed to advertise", e);
	        }
	}

	@Override
	public void stop() {
        // Unregister all services
        jmdns.unregisterAllServices();
	}
}
