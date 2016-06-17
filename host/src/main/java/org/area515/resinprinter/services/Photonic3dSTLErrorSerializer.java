package org.area515.resinprinter.services;

import java.io.IOException;

import org.area515.resinprinter.slice.StlError;
import org.area515.resinprinter.slice.StlError.ErrorType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class Photonic3dSTLErrorSerializer extends JsonSerializer<StlError> {
	@Override
	public void serialize(StlError value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		if (value.getType() == ErrorType.NonManifold) {
			jgen.writeStartObject();
			jgen.writeNumberField("i", value.getTriangle().getOriginalIndex());
			jgen.writeEndObject();
		}
	}
}
