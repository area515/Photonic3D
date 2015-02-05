package org.area515.jackson.util;

import java.io.IOException;

import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JacksonEncoder implements Encoder.Text<Object>{

    private ObjectMapper mapper;

    public JacksonEncoder() {
		mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
    
    @Override
    public String encode(Object m) {
        try {
            return mapper.writeValueAsString(m);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public void destroy() {
	}

	@Override
	public void init(EndpointConfig arg0) {
	}
}