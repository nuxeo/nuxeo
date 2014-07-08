package com.onelogin.saml;

import com.onelogin.AccountSettings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Response {

	private Document xmlDoc;
	private Integer Assertions;
	private Element rootElement;
	private final AccountSettings accountSettings;
	private final Certificate certificate;

	public Response(AccountSettings accountSettings) throws CertificateException {
		this.accountSettings = accountSettings;
		certificate = new Certificate();
		certificate.loadCertificate(this.accountSettings.getCertificate());
	}

	public void loadXml(String xml) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory fty = DocumentBuilderFactory.newInstance();
		fty.setNamespaceAware(true);
		// XMLConstants with FEATURE_SECURE_PROCESSING prevents external document access. (XXE/XEE Possible Attacks).
		fty.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		DocumentBuilder builder = fty.newDocumentBuilder();
		ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
		xmlDoc = builder.parse(bais);
	}

	public void loadXmlFromBase64(String response) throws ParserConfigurationException, SAXException, IOException {
		Base64 base64 = new Base64();
		byte[] decodedB = base64.decode(response);
		String decodedS = new String(decodedB);
		loadXml(decodedS);
	}

	// isValid() function should be called to make basic security checks to responses.
	public boolean isValid() throws Exception {
		// Security Checks
		rootElement = xmlDoc.getDocumentElement();
		Assertions = xmlDoc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion").getLength();
		xmlDoc.getDocumentElement().normalize();

		if (Assertions > 1) {
			throw new Exception("SAML Response must contain 1 Assertion.");
		}

		String attName = rootElement.getAttribute("ID");
		if (attName.equals("")) {
			throw new Exception("Missing ID attribute on SAML Response.");
		}

		attName = rootElement.getAttribute("Version");
		if (!attName.equals("2.0")) {
			throw new Exception("Unsupported SAML Version.");
		}

		NodeList nodes = xmlDoc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

		if (nodes == null || nodes.getLength() == 0) {
			throw new Exception("Can't find signature in Document.");
		}

		if (setIdAttributeExists()) {
//			tagIdAttributes(xmlDoc);
		}

		X509Certificate cert = certificate.getX509Cert();
		DOMValidateContext ctx = new DOMValidateContext(cert.getPublicKey(), nodes.item(0));
		XMLSignatureFactory sigF = XMLSignatureFactory.getInstance("DOM");
		XMLSignature xmlSignature = sigF.unmarshalXMLSignature(ctx);

		return xmlSignature.validate(ctx);
	}

	public String getNameId() throws Exception {
		NodeList nodes = xmlDoc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
		if (nodes.getLength() == 0) {
			throw new Exception("No name id found in Document.");
		}
		return nodes.item(0).getTextContent();
	}

	public String getAttribute(String name) {
		HashMap attributes = getAttributes();
		if (!attributes.isEmpty()) {
			return attributes.get(name).toString();
		}
		return null;
	}

	public HashMap getAttributes() {
		HashMap<String, ArrayList> attributes = new HashMap<>();
		NodeList nodes = xmlDoc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Attribute");

		if (nodes.getLength() != 0) {
			for (int i = 0; i < nodes.getLength(); i++) {
				NamedNodeMap attrName = nodes.item(i).getAttributes();
				String attName = attrName.getNamedItem("Name").getNodeValue();
				NodeList children = nodes.item(i).getChildNodes();

				ArrayList<String> attrValues = new ArrayList<String>();
				for (int j = 0; j < children.getLength(); j++) {
					attrValues.add(children.item(j).getTextContent());
				}
				attributes.put(attName, attrValues);
			}
		} else {
			return null;
		}
		return attributes;
	}

	private boolean setIdAttributeExists() {
		for (Method method : Element.class.getDeclaredMethods()) {
			if (method.getName().equals("setIdAttribute")) {
				return true;
			}
		}
		return false;
	}

	private void tagIdAttributes(Document xmlDoc) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
