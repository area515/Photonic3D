package org.area515.resinprinter.network;

import java.lang.StringBuilder;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.apache.commons.lang3.text.translate.NumericEntityUnescaper;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;
import org.area515.util.IOUtilities;
import org.area515.util.IOUtilities.ParseAction;
import org.area515.util.IOUtilities.SearchStyle;

public class LinuxNetworkManager implements NetworkManager {
	public static final String WIFI_REGEX = "\\s*([A-Fa-f0-9:]+)\\s+(-?\\d+)\\s+(-?\\d+)\\s+([\\[\\]\\+\\-\\w]+)\\t(.+)";
	
	public Map getMACs(){
		Map MACs = new HashMap();
		
		try {
			for (Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces(); networks.hasMoreElements();){
					NetworkInterface network = networks.nextElement();
					byte[] mac = network.getHardwareAddress();
					if (mac != null && mac.length > 0){
						StringBuilder sb = new StringBuilder(18);
						for (byte b : mac) {
							if (sb.length() > 0) sb.append(':');
							sb.append(String.format("%02x", b));
						}
						MACs.put(network.getName().trim(), sb.toString());
					}
			}
		} catch (SocketException e){
			// can't get the info
		}
		// no-one cares about loopback
		if (MACs.containsKey("lo")) MACs.remove("lo");
		// photocentric specific code
		// if (MACs.containsKey("wlan1")) MACs.remove("wlan0");
		return MACs;
	}

	public Map getIPs(){
		Map IPs = new HashMap();
		
		try {
			for (Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces(); networks.hasMoreElements();){
					NetworkInterface network = networks.nextElement();
					String IPaddress = null;
					// find the IPv4 address in the Enumeration
					for (Enumeration<InetAddress> ips = network.getInetAddresses(); ips.hasMoreElements();){
						String check = ips.nextElement().getHostAddress();
						if(check.indexOf(".")>=0 && check.indexOf(".") < 4){
							IPaddress = check;
						}
					}
					if (IPaddress != null) IPs.put(network.getName().trim(), IPaddress);
			}
		} catch (SocketException e){
			// can't get the info
		}
		// noone cares about loopback
		if (IPs.containsKey("lo")) IPs.remove("lo");
		// photocentric specific code
		// if (IPs.containsKey("wlan1")) IPs.remove("wlan0");
		return IPs;
	}
	
	public String getHostname(){
		String[] output = IOUtilities.executeNativeCommand(new String[]{"hostname"}, null, (String) null);
		return output[0];
	}

    public static final CharSequenceTranslator UNESCAPE_UNIX = 
            new AggregateTranslator(
                new LookupTranslator(EntityArrays.BASIC_UNESCAPE()),
                new LookupTranslator(EntityArrays.ISO8859_1_UNESCAPE()),
                new LookupTranslator(EntityArrays.HTML40_EXTENDED_UNESCAPE()),
                new NumericEntityUnescaper(),       //&#9786;
                //new OctalUnescaper(),             // .between('\1', '\377'),
                new UnicodeUnescaper(),             //\u0044
                new HexUnescaper(),                 //\x45
                //new LookupTranslator(EntityArrays.JAVA_CTRL_CHARS_UNESCAPE()),
                new LookupTranslator(
                          new String[][] { 
                                {"\\\\", "\\"},
                                {"\\\"", "\""},
                                {"\\'", "'"},
                                {"\\", ""}
                          })
            );
	
	private void buildWirelessInfo(String nicName, NetInterface netFace) {
		Pattern networkEncryptionClass = Pattern.compile("\\[([\\+\\-\\w]+)\\]");

		List<ParseAction> parseActions = new ArrayList<ParseAction>();
		parseActions.add(new ParseAction(new String[]{"wpa_cli", "-i", "{0}"}, ">", SearchStyle.RepeatUntilMatch));
		parseActions.add(new ParseAction(new String[]{"scan\n"}, "[\\s\r]*<\\d+>\\s*CTRL-EVENT-SCAN-RESULTS\\s*", SearchStyle.RepeatUntilMatch));
		parseActions.add(new ParseAction(new String[]{""}, "\\s*>", SearchStyle.RepeatUntilMatch));
		parseActions.add(new ParseAction(new String[]{"scan_results\n"}, "bssid.*", SearchStyle.RepeatUntilMatch));
		parseActions.add(new ParseAction(new String[]{""}, WIFI_REGEX, SearchStyle.RepeatWhileMatching));
		
		List<String[]> output = IOUtilities.communicateWithNativeCommand(parseActions, "^>|\n", true, null, nicName);
		for (String[] lines : output) {
			if (lines == null) {
				continue;
			}
			
			WirelessNetwork currentWireless = new WirelessNetwork();
			netFace.getWirelessNetworks().add(currentWireless);
			currentWireless.setSsid(UNESCAPE_UNIX.translate(lines[4]));
			if (currentWireless.getSsid().startsWith("\u0000")) {
				currentWireless.setHidden(true);
			}
			currentWireless.setParentInterfaceName(netFace.getName());
			currentWireless.setSignalStrength(lines[2]);
			Matcher matcher = networkEncryptionClass.matcher(lines[3]);
			while (matcher.find()) {
				StringTokenizer tokenizer = new StringTokenizer(matcher.group(1), "+-");
				String flag = tokenizer.nextToken();
				if (flag.equals("WEP")) {
					WirelessEncryption encryption = new WirelessEncryption();
					currentWireless.getSupportedWirelessEncryption().add(encryption);
					encryption.setEncryptionClass(EncryptionClass.WEP);
				} else if (flag.startsWith("WPA")) {
					WirelessEncryption encryption = new WirelessEncryption();
					currentWireless.getSupportedWirelessEncryption().add(encryption);
					encryption.setEncryptionClass(EncryptionClass.valueOf(flag));
					while (tokenizer.hasMoreTokens()) {
						flag = tokenizer.nextToken();
						if (flag.equals("TKIP") || flag.equals("CCMP")) {
							encryption.getGroupCipher().add(WirelessCipher.valueOf(flag));
							encryption.getPairwiseCipher().add(WirelessCipher.valueOf(flag));
						}
					}
				} else if (flag.equals("ESS")) {
					//TODO:
				} else if (flag.equals("WPS")) {
					//TODO:
				}
			}
		}

	}
	
	@Override
	public List<NetInterface> getNetworkInterfaces() {
		List<NetInterface> ifaces = new ArrayList<NetInterface>();
		String[] nics = IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "ifconfig | grep Link | awk '''{ print $1 }'''"}, null);
		
		for (String nicName : nics) {
			NetInterface netFace = new NetInterface();
			netFace.setName(nicName);
			ifaces.add(netFace);
			
			Boolean doneLookingForWifi = null;
			while (doneLookingForWifi == null || !doneLookingForWifi) {
				String[] wpaSupplicants = IOUtilities.executeNativeCommand(new String[]{"wpa_cli", "-i", "{0}", "ping"}, null, nicName);
				if (wpaSupplicants.length > 0 && wpaSupplicants[0].trim().equals("PONG")) {
					buildWirelessInfo(nicName, netFace);
					doneLookingForWifi = true;
				} else if (doneLookingForWifi == null) {
					IOUtilities.executeNativeCommand(new String[]{"ifup", "{0}"}, null, nicName);
					doneLookingForWifi = false;
				} else {
					doneLookingForWifi = true;
				}
			}
		}
		
		return ifaces;
	}

	@Override
	public void connectToWirelessNetwork(WirelessNetwork wireless) {
		/*String[] configuredNetworkIds = IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "wpa_cli -i {0} list_network | grep -v \"network id / ssid / bssid / flags\" | awk '''{print $1}'''"}, wireless.getParentInterfaceName());
		for (String networkId : configuredNetworkIds) {
			//We are going to take over the first network
			if (networkId.equals("0")) {
				can't do this!
				String[] okFail = IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "wpa_cli -i {0} remove_network 0"}, wireless.getParentInterfaceName());
				if (!okFail[0].equals("OK")) {
					throw new IllegalArgumentException("I wasn't able to remove your wireless network id:0 in order to reconfigure it");
				}
				break;
			}
		}*/
		
		WirelessEncryption encryption = null;
		for (WirelessEncryption e : wireless.getSupportedWirelessEncryption()) {
			if (encryption == null || e.getEncryptionClass().getPriority() > encryption.getEncryptionClass().getPriority()) {
				encryption = e;
			}
		}
		
		List<ParseAction> parseActions = new ArrayList<ParseAction>();
		parseActions.add(new ParseAction(new String[]{"wpa_cli", "-i", "{0}"}, ">", SearchStyle.RepeatUntilMatch));
		parseActions.add(new ParseAction(new String[]{"add_network\n"}, "\\s*>", "\\s*(\\d+)\\s*", SearchStyle.RepeatUntilMatch));
		parseActions.add(new ParseAction(new String[]{"set_network {4} ssid \"{1}\"\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
		parseActions.add(new ParseAction(new String[]{"set_network {4} id_str \"ManagedByPhotonic3D\"\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
		switch (encryption.getEncryptionClass() == null?EncryptionClass.Open:encryption.getEncryptionClass()) {
			case WEP:
				parseActions.add(new ParseAction(new String[]{"set_network {4} key_mgmt NONE\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				parseActions.add(new ParseAction(new String[]{"set_network {4} auth_alg OPEN\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				parseActions.add(new ParseAction(new String[]{"set_network {4} wep_key0 {2}\n"}, "\\s*(?:>|(FAIL|OK))", SearchStyle.RepeatUntilMatchWithNullGroup));
				break;
			case WPA:
				parseActions.add(new ParseAction(new String[]{"set_network {4} psk \"{2}\"\n"}, "\\s*(?:>|(FAIL|OK))", SearchStyle.RepeatUntilMatchWithNullGroup));
				parseActions.add(new ParseAction(new String[]{"set_network {4} proto WPA\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				parseActions.add(new ParseAction(new String[]{"set_network {4} key_mgmt WPA-PSK\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				parseActions.add(new ParseAction(new String[]{"set_network {4} auth_alg OPEN\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				parseActions.add(new ParseAction(new String[]{"set_network {4} pairwise {3}\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				break;
			case WPA2:
				parseActions.add(new ParseAction(new String[]{"set_network {4} psk \"{2}\"\n"}, "\\s*(?:>|(FAIL|OK))", SearchStyle.RepeatUntilMatchWithNullGroup));
				parseActions.add(new ParseAction(new String[]{"set_network {4} proto RSN\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				parseActions.add(new ParseAction(new String[]{"set_network {4} key_mgmt WPA-PSK\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				parseActions.add(new ParseAction(new String[]{"set_network {4} auth_alg OPEN\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				parseActions.add(new ParseAction(new String[]{"set_network {4} pairwise {3}\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				break;
			case Open:
				parseActions.add(new ParseAction(new String[]{"set_network {4} key_mgmt NONE\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				parseActions.add(new ParseAction(new String[]{"set_network {4} auth_alg OPEN\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
				break;
		}
		
		//TODO: Complete when you get a CTRL-EVENT-CONNECTED
		parseActions.add(new ParseAction(new String[]{"select_network {4}\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));//Enables this network and disables the rest!
		parseActions.add(new ParseAction(new String[]{"save_config\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
		parseActions.add(new ParseAction(new String[]{"reconfigure\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
		parseActions.add(new ParseAction(new String[]{"quit\n"}, "\\s*>", SearchStyle.RepeatUntilMatch));
		
		List<String[]> exitValues = IOUtilities.communicateWithNativeCommand(parseActions, "^>|\n", true, null, wireless.getParentInterfaceName(), wireless.getSsid(), wireless.getPassword(), encryption.getPairwiseCipher().size() > 0?(encryption.getPairwiseCipher().get(0) + ""):null);
		if (exitValues.size() > 0) {
			String[] passwordGroups = exitValues.get(0);
			if (passwordGroups.length > 0 && passwordGroups[0].equals("FAIL")) {
				throw new IllegalArgumentException("Unable to set password on wifi network.");
			}
		}
	}
	
	public void setHostname(String newHostname){
		// do the new /etc/hosts hostname first.
		String[] macResults = IOUtilities.executeNativeCommand(new String[]{"bash", "-c", "sed -i \"s/$(hostname)/"+newHostname+"/g\" /etc/hosts"}, null, (String) null);
		// then the easier one - /etc/hostname
		macResults = IOUtilities.executeNativeCommand(new String[]{"bash", "-c", "echo "+newHostname+" > /etc/hostname"}, null, (String) null);
		// get hostname to acknowledge the new hostname
		macResults = IOUtilities.executeNativeCommand(new String[]{"hostname", "-F", "/etc/hostname"}, null, (String) null);
		// how to handle restarts...? Perhaps hand that off to the user.
	}
	
	//TODO: Should this use Unix escaping?
	public String getCurrentSSID(){
		// Can use iwgetid -r to get a basic SSID
		String[] output = IOUtilities.executeNativeCommand(new String[]{"iwgetid", "-r"}, null, (String) null);
		if (output.length > 0) {
			return output[0];
		}
		else {
			return null;
		}
	}
}
