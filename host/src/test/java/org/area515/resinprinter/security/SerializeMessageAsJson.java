package org.area515.resinprinter.security;

import java.io.IOException;
import java.util.UUID;

import org.area515.resinprinter.util.security.Message;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializeMessageAsJson {
	@Test
	public void testSerializeDeserializeMessage() throws JsonParseException, JsonMappingException, IOException {
		UUID from = UUID.randomUUID();
		UUID to = UUID.randomUUID();
		String encryptionAlgorithm = "justAString";
		String signatureBytes = "bytesAsStrings\n";
		String dataBytes = "lotsOfDataInHere\n";
		
		Message message = new Message();
		message.setFrom(from);
		message.setTo(to);
		message.setEncryptionAlgorithm(encryptionAlgorithm);
		message.setIvOffset(null);
		message.setSignature(signatureBytes.getBytes());
		message.setData(dataBytes.getBytes());
		
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		byte[] objectAsJSon = mapper.writeValueAsBytes(message);
		Message newMessage = mapper.readValue(objectAsJSon, Message.class);
		Assert.assertEquals(message.getFrom(), newMessage.getFrom());
		Assert.assertEquals(message.getTo(), newMessage.getTo());
		Assert.assertEquals(message.getEncryptionAlgorithm(), newMessage.getEncryptionAlgorithm());
		Assert.assertEquals(message.getIvOffset(), newMessage.getIvOffset());
		Assert.assertArrayEquals(message.getSignature(), newMessage.getSignature());
		Assert.assertArrayEquals(message.getData(), newMessage.getData());
		Assert.assertArrayEquals(message.getSignature(), newMessage.getSignature());
	}
}
