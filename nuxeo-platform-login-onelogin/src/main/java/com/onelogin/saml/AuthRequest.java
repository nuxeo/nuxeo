package com.onelogin.saml;

import com.onelogin.AccountSettings;
import com.onelogin.AppSettings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.codec.binary.Base64;

public class AuthRequest {

	private final String id;
	private final String issueInstant;
	private final AppSettings appSettings;
	public static final int base64 = 1;
	private Deflater deflater;

	public AuthRequest(AppSettings appSettings, AccountSettings accountSettings){
		this.appSettings = appSettings;
		id="_"+UUID.randomUUID().toString();
		SimpleDateFormat simpleDf = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss'Z'");
		issueInstant = simpleDf.format(new Date());
	}

	public String getRequest(int format) throws XMLStreamException, IOException {
		String result = "";

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(baos);

		writer.writeStartElement("samlp", "AuthnRequest", "urn:oasis:names:tc:SAML:2.0:protocol");
		writer.writeNamespace("samlp","urn:oasis:names:tc:SAML:2.0:protocol");

		writer.writeAttribute("ID", id);
		writer.writeAttribute("Version", "2.0");
		writer.writeAttribute("IssueInstant", this.issueInstant);
		writer.writeAttribute("ProtocolBinding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
		writer.writeAttribute("AssertionConsumerServiceURL", this.appSettings.getAssertionConsumerServiceUrl());

		writer.writeStartElement("saml","Issuer","urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeNamespace("saml","urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeCharacters(this.appSettings.getIssuer());
		writer.writeEndElement();

		writer.writeStartElement("samlp", "NameIDPolicy", "urn:oasis:names:tc:SAML:2.0:protocol");

		writer.writeAttribute("Format", "urn:oasis:names:tc:SAML:2.0:nameid-format:unspecified");
		writer.writeAttribute("AllowCreate", "true");
		writer.writeEndElement();

		writer.writeStartElement("samlp","RequestedAuthnContext","urn:oasis:names:tc:SAML:2.0:protocol");

		writer.writeAttribute("Comparison", "exact");

		writer.writeStartElement("saml","AuthnContextClassRef","urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
		writer.writeCharacters("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
		writer.writeEndElement();

		writer.writeEndElement();
		writer.writeEndElement();
		writer.flush();
                
		result = encodeSAMLRequest(baos.toByteArray());
		return result;
	}

	public static String getRidOfCRLF(String what) {
		String lf = "%0D";
		String cr = "%0A";
		String now = lf;
		int index = what.indexOf(now);

		StringBuilder r = new StringBuilder();

		while (index!=-1) {
			r.append(what.substring(0,index));
			what = what.substring(index+3,what.length());

			if (now.equals(lf)) {
				now = cr;
			} else {
				now = lf;
			}

			index = what.indexOf(now);
		}
		return r.toString();
	}

	private String encodeSAMLRequest(byte[] pSAMLRequest) throws RuntimeException {

		Base64 base64Encoder = new Base64();

		try {
			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
			Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

			DeflaterOutputStream def = new DeflaterOutputStream(byteArray, deflater);
			def.write(pSAMLRequest);
			def.close();
			byteArray.close();

			String stream = new String(base64Encoder.encode(byteArray.toByteArray()));

			return stream;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
