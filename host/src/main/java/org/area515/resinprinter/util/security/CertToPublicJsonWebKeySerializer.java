package org.area515.resinprinter.util.security;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.naming.InvalidNameException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class CertToPublicJsonWebKeySerializer extends JsonSerializer<Certificate> {
	@Override
	public void serialize(Certificate value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		if (!(value instanceof X509Certificate)) {
			throw new JsonGenerationException("I don't understand an object of type:" + value.getClass());
		}
		
		X509Certificate xcert = (X509Certificate)value;
		if (!(xcert.getPublicKey() instanceof RSAPublicKey)) {
			throw new JsonGenerationException("I don't understand an object of type:" + xcert.getPublicKey().getClass());
		}
		
		RSAPublicKey rsaPublic = (RSAPublicKey)xcert;
		jgen.writeStartObject();
		jgen.writeFieldName("kty");
		jgen.writeString("RSA");
		jgen.writeFieldName("e");
		jgen.writeBinary(rsaPublic.getModulus().toByteArray());
		jgen.writeFieldName("n");
		jgen.writeBinary(rsaPublic.getPublicExponent().toByteArray());
		jgen.writeFieldName("alg");
		jgen.writeString("RS256");
		jgen.writeFieldName("ext");
		jgen.writeString("true");
		jgen.writeFieldName("kid");
		try {
			jgen.writeString(LdapUtils.getUserIdAndName(xcert.getSubjectDN().getName())[0]);
		} catch (InvalidNameException e) {
			throw new JsonGenerationException("Certificate must have a uid name component in it's subject DN");
		}
		jgen.writeEndObject();
	}
}
