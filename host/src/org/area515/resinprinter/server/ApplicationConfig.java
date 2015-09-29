package org.area515.resinprinter.server;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.area515.resinprinter.services.FileService;
import org.area515.resinprinter.services.MachineService;
import org.area515.resinprinter.services.MediaService;
import org.area515.resinprinter.services.PrinterService;
import org.area515.resinprinter.services.SettingsService;
import org.area515.util.ExceptionMarshaller;
import org.area515.util.PrioritizeJSONPropertyJaxbAnnotationIntrospector;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

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
    	singletons.add(buildJacksonJaxbJsonProvider());
    	singletons.add(new ExceptionMarshaller());
    	singletons.add(FileService.INSTANCE);
    	singletons.add(MachineService.INSTANCE);
    	singletons.add(SettingsService.INSTANCE);
    	singletons.add(PrinterService.INSTANCE);
    	singletons.add(MediaService.INSTANCE);
    }

    public JacksonJaxbJsonProvider buildJacksonJaxbJsonProvider() {
        ObjectMapper mapper = new ObjectMapper();
        //mapper.enable(SerializationFeature.INDENT_OUTPUT);
        AnnotationIntrospector pair = AnnotationIntrospector.pair(
        		new PrioritizeJSONPropertyJaxbAnnotationIntrospector(), 
        		new JacksonAnnotationIntrospector());
        mapper.getDeserializationConfig().with(pair);
        mapper.getSerializationConfig().with(pair);
        mapper.setAnnotationIntrospectors(pair, pair);
        // create JsonProvider to provide custom ObjectMapper
        ResteasyJackson2Provider provider = new ResteasyJackson2Provider();
        provider.setMapper(mapper);
        return provider;
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
