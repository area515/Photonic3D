package org.area515.resinprinter.inkdetection;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class PrintMaterialDetectorSettings {
	private HashMap<String, String> settings;
	
    @JsonAnyGetter
    public HashMap<String, String> getSettings() {
        return settings;
    }
    
    public void setSettings(HashMap<String, String> settings) {
        this.settings = settings;
    }
    
    @JsonAnySetter
    public void putSettings(String key, String value) {
        if (settings == null) {
        	settings = new HashMap<>();
        }
        
        settings.put(key, value);
    }
}
