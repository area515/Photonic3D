package org.area515.util;

import java.io.IOException;

import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import org.area515.resinprinter.job.PrintJob;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PrintJobJacksonDecoder implements Decoder.Text<PrintJob> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public PrintJob decode(String s) {
        try {
            return mapper.readValue(s, PrintJob.class);
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

	@Override
	public boolean willDecode(String arg0) {
		return false;
	}
}