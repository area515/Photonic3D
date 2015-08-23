package org.area515.resinprinter.server;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.area515.resinprinter.services.FileService;
import org.area515.resinprinter.services.MachineService;
import org.area515.resinprinter.services.MediaService;
import org.area515.resinprinter.services.PrinterService;
import org.area515.resinprinter.services.SettingsService;

public class ApplicationConfig extends Application{

//	private static Set services = new HashSet(); 
//	 public  ApplicationConfig() {     
//	   // initialize restful services   
//	   services.add(new RfidService());  
//	 }
//	 @Override
//	 public  Set getSingletons() {
//	  return services;
//	 }  
//	 public  static Set getServices() {  
//	  return services;
//	 } 
	
	private Set<Object> singletons = new HashSet<Object>();
    private Set<Class<?>> classes = new HashSet<Class<?>>();

    public ApplicationConfig() {
    	singletons.add(FileService.INSTANCE);
    	singletons.add(MachineService.INSTANCE);
    	singletons.add(SettingsService.INSTANCE);
    	singletons.add(PrinterService.INSTANCE);
    	singletons.add(MediaService.INSTANCE);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
	
}
