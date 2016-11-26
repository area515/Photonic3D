package org.area515.resinprinter.server;

import io.swagger.jaxrs.config.BeanConfig;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.area515.resinprinter.services.CustomizerService;
import org.area515.resinprinter.services.MachineService;
import org.area515.resinprinter.services.MediaService;
import org.area515.resinprinter.services.PrintJobService;
import org.area515.resinprinter.services.PrintableService;
import org.area515.resinprinter.services.PrinterService;
import org.area515.resinprinter.services.RemoteService;
import org.area515.resinprinter.services.SettingsService;
import org.area515.resinprinter.services.UserService;
import org.area515.util.ExceptionMarshaller;
import org.area515.util.PrioritizeJSONPropertyJaxbAnnotationIntrospector;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

public class ApplicationConfig extends Application{
	private Set<Object> singletons = new HashSet<Object>();

    public ApplicationConfig() {
    	BeanConfig beanConfig = new BeanConfig();
    	beanConfig.setTitle("Photonic3D REST API");
        beanConfig.setVersion(HostProperties.Instance().getReleaseTagName());
        beanConfig.setSchemes(new String[]{"http"});
        //beanConfig.setHost("localhost:9091");
        beanConfig.setBasePath("/services");
        beanConfig.setResourcePackage("org.area515.resinprinter.services");
        beanConfig.setScan(true);
        beanConfig.setPrettyPrint(true);

    	singletons.add(buildJacksonJaxbJsonProvider());
    	singletons.add(new ExceptionMarshaller());
    	singletons.add(PrintableService.INSTANCE);
    	singletons.add(MachineService.INSTANCE);
    	singletons.add(SettingsService.INSTANCE);
    	singletons.add(PrinterService.INSTANCE);
    	singletons.add(PrintJobService.INSTANCE);
    	singletons.add(MediaService.INSTANCE);
    	singletons.add(CustomizerService.INSTANCE);
    	singletons.add(UserService.INSTANCE);
    	singletons.add(RemoteService.INSTANCE);
    }

    public static JacksonJaxbJsonProvider buildJacksonJaxbJsonProvider() {
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
        HashSet<Class<?>> resources = new HashSet<Class<?>>();
        
        resources.add(io.swagger.jaxrs.listing.ApiListingResource.class);
        resources.add(io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.class);
        resources.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        //resources.add(io.swagger.jaxrs.listing.ApiDeclarationProvider.class);
        //resources.add(io.swagger.jaxrs.listing.ApiListingResourceJSON.class);
        //resources.add(io.swagger.jaxrs.listing.ResourceListingProvider.class);
        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
	
}
