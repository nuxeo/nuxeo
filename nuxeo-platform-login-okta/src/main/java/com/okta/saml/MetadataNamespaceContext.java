package com.okta.saml;

/**
 * NamespaceContext used for XPath operations
 */

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class MetadataNamespaceContext implements NamespaceContext {
    private static Map<String, String> NAMESPACES;
    static {
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("saml2p", "urn:oasis:names:tc:SAML:2.0:protocol");
        namespaces.put("saml2", "urn:oasis:names:tc:SAML:2.0:assertion");
        namespaces.put("md", "urn:oasis:names:tc:SAML:2.0:metadata");
        namespaces.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("ec", "http://www.w3.org/2001/10/xml-exc-c14n#");
        namespaces.put("xml", XMLConstants.XML_NS_URI);
        NAMESPACES = Collections.unmodifiableMap(namespaces);
    }

    public String getNamespaceURI(String prefix) {
        String namespace = NAMESPACES.get(prefix);

        if (namespace == null) {
            return XMLConstants.NULL_NS_URI;
        } else {
            return namespace;
        }
    }

    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }

}
