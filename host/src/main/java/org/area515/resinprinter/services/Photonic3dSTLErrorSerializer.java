package org.area515.resinprinter.services;

import java.io.IOException;

import org.area515.resinprinter.slice.StlError;
import org.area515.resinprinter.slice.StlError.ErrorType;
import org.area515.resinprinter.stl.MultiTriangleFace;
import org.area515.resinprinter.stl.Triangle3d;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class Photonic3dSTLErrorSerializer extends JsonSerializer<StlError> {
	@Override
	public void serialize(StlError value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		if (value.getType() == ErrorType.NonManifold) {
			if (value.getFace() instanceof Triangle3d) {
				jgen.writeStartObject();
				jgen.writeNumberField("i", ((Triangle3d)value.getFace()).getOriginalIndex());
				jgen.writeEndObject();
			} else if (value.getFace() instanceof MultiTriangleFace) {
				for (Triangle3d tri : ((MultiTriangleFace)value.getFace()).getFaces()) {
					jgen.writeStartObject();
					jgen.writeNumberField("i", tri.getOriginalIndex());
					jgen.writeEndObject();
				}
			}
		}
	}
}
