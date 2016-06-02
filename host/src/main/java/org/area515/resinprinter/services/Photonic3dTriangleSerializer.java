package org.area515.resinprinter.services;

import java.io.IOException;

import org.area515.resinprinter.stl.Point3d;
import org.area515.resinprinter.stl.Triangle3d;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


public class Photonic3dTriangleSerializer extends JsonSerializer<Triangle3d> {
	@Override
	public void serialize(Triangle3d value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeStartObject();
		jgen.writeFieldName("v");
		jgen.writeStartArray();
		for (Point3d point : value.getPoints()) {
			jgen.writeNumber(point.x);
			jgen.writeNumber(point.y);
			jgen.writeNumber(point.z);
		}
		jgen.writeEndArray();
		jgen.writeFieldName("n");
		jgen.writeStartArray();
		
		Point3d normal = value.getNormal();
		jgen.writeNumber(normal.x);
		jgen.writeNumber(normal.y);
		jgen.writeNumber(normal.z);
		
		jgen.writeEndArray();
		jgen.writeEndObject();
	}
}
