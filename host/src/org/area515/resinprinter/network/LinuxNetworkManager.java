package org.area515.resinprinter.network;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.area515.resinprinter.network.NetInterface.EncryptionClass;
import org.area515.resinprinter.network.NetInterface.WirelessCipher;
import org.area515.resinprinter.network.NetInterface.WirelessEncryption;
import org.area515.resinprinter.network.NetInterface.WirelessNetwork;
import org.area515.util.IOUtilities;
import org.area515.util.IOUtilities.ParseAction;
import org.area515.util.IOUtilities.SearchStyle;

public class LinuxNetworkManager implements NetworkManager {
	private void buildWirelessInfo(String nicName, NetInterface netFace) {
		Pattern networkEncryptionClass = Pattern.compile("\\[([\\+\\-\\w]+)\\]");

		List<ParseAction> parseActions = new ArrayList<ParseAction>();
		parseActions.add(new ParseAction(new String[]{"wpa_cli", "-i", "{0}"}, ">", SearchStyle.RepeatUntilFound));
		parseActions.add(new ParseAction(new String[]{"scan\n"}, "[\\s\r]*<\\d+>\\s*CTRL-EVENT-SCAN-RESULTS\\s*", SearchStyle.RepeatUntilFound));
		parseActions.add(new ParseAction(new String[]{""}, "\\s*>", SearchStyle.RepeatUntilFound));
		parseActions.add(new ParseAction(new String[]{"scan_results\n"}, "bssid.*", SearchStyle.RepeatUntilFound));
		parseActions.add(new ParseAction(new String[]{""}, "\\s*([A-Fa-f0-9:]+)\\s+(\\d+)\\s+(\\d+)\\s+([\\[\\]\\+\\-\\w]+)\\s+(\\w*)\\s*", SearchStyle.RepeatWhileFound));
		
		List<String[]> output = IOUtilities.communicateWithNativeCommand(parseActions, "^>|\n", true, null, nicName);
		for (String[] lines : output) {
			if (lines == null) {
				continue;
			}
			
			WirelessNetwork currentWireless = new WirelessNetwork();
			netFace.getWirelessNetworks().add(currentWireless);
			currentWireless.setSsid(lines[4]);
			currentWireless.setParentInterface(netFace);
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
			
			String[] wpaSupplicants = IOUtilities.executeNativeCommand(new String[]{"wpa_cli", "-i", "{0}", "ping"}, null, nicName);
			if (wpaSupplicants.length > 0 && wpaSupplicants[0].trim().equals("PONG")) {
				buildWirelessInfo(nicName, netFace);
			}
		}
		
		return ifaces;
	}

	@Override
	public void connectToWirelessNetwork(WirelessNetwork wireless, String password) {
		String[] configuredNetworkIds = IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "wpa_cli -i {0} list_network | grep -v \"network id / ssid / bssid / flags\" | awk '''{print $1}'''"}, wireless.getParentInterface().getName());
		for (String networkId : configuredNetworkIds) {
			//We are going to take over the first network
			if (networkId.equals("0")) {
				String[] okFail = IOUtilities.executeNativeCommand(new String[]{"/bin/sh", "-c", "wpa_cli -i {0} remove_network 0"}, wireless.getParentInterface().getName());
				if (!okFail[0].equals("OK")) {
					throw new IllegalArgumentException("I wasn't able to remove your wireless network id:0 in order to reconfigure it");
				}
				break;
			}
		}
		
		WirelessEncryption encryption = null;
		for (WirelessEncryption e : wireless.getSupportedWirelessEncryption()) {
			if (encryption == null || encryption.getEncryptionClass().getPriority() > encryption.getEncryptionClass().getPriority()) {
				encryption = e;
			}
		}
		
		List<ParseAction> parseActions = new ArrayList<ParseAction>();
		parseActions.add(new ParseAction(new String[]{"wpa_cli", "-i", "{0}"}, ">", SearchStyle.RepeatUntilFound));
		parseActions.add(new ParseAction(new String[]{"add_network\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
		parseActions.add(new ParseAction(new String[]{"set_network 0 ssid \"{1}\"\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
		parseActions.add(new ParseAction(new String[]{"set_network 0 id_str \"ManagedByCWH\"\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
		switch (encryption.getEncryptionClass() == null?EncryptionClass.Open:encryption.getEncryptionClass()) {
			case WEP:
				parseActions.add(new ParseAction(new String[]{"set_network 0 key_mgmt NONE\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 auth_alg OPEN\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 wep_key0 {2}\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				break;
			case WPA:
				parseActions.add(new ParseAction(new String[]{"set_network 0 psk \"{2}\"\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 proto WPA\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 key_mgmt WPA-PSK\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 auth_alg OPEN\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 pairwise {3}\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				break;
			case WPA2:
				parseActions.add(new ParseAction(new String[]{"set_network 0 psk \"{2}\"\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 proto RSN\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 key_mgmt WPA-PSK\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 auth_alg OPEN\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 pairwise {3}\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				break;
			case Open:
				parseActions.add(new ParseAction(new String[]{"set_network 0 key_mgmt NONE\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				parseActions.add(new ParseAction(new String[]{"set_network 0 auth_alg OPEN\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
				break;
		}
		
		//TODO: Complete when you get a CTRL-EVENT-CONNECTED
		parseActions.add(new ParseAction(new String[]{"select_network 0\n"}, "\\s*>", SearchStyle.RepeatUntilFound));//Enables this network and disables the rest!
		parseActions.add(new ParseAction(new String[]{"save_config\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
		parseActions.add(new ParseAction(new String[]{"reconfigure\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
		parseActions.add(new ParseAction(new String[]{"quit\n"}, "\\s*>", SearchStyle.RepeatUntilFound));
		
		IOUtilities.communicateWithNativeCommand(parseActions, "^>|\n", true, null, wireless.getParentInterface().getName(), wireless.getSsid(), password, encryption.getPairwiseCipher().size() > 0?(encryption.getPairwiseCipher().get(0) + ""):null);
	}
}
