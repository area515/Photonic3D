package org.area515.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

public class PrioritizeJSONPropertyJaxbAnnotationIntrospector extends JaxbAnnotationIntrospector {
	private static final long serialVersionUID = -2938408706787731561L;
	
	public PrioritizeJSONPropertyJaxbAnnotationIntrospector(){
		super(TypeFactory.defaultInstance());
	}
	
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        if (m.hasAnnotation(JsonProperty.class) ) {
            return false;
        }
            
        return super.hasIgnoreMarker(m);
    }
}
