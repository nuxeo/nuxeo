/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Contributors:
 *     Arnaud Kervern
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.impl.webservices.AbstractService;

/**
 * Extracts username and password from a UsernameToken
 *
 * @since 5.7.3
 */
public class CXFAuthHandler implements SOAPHandler<SOAPMessageContext> {

    protected static final JAXBContext WSSE_CONTEXT;

    static {
        try {
            WSSE_CONTEXT = JAXBContext.newInstance(ObjectFactory.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    protected static final QName WSSE_SECURITY = new QName(WSSE_NS, "Security");

    protected static final QName WSSE_USERNAME_TOKEN = new QName(WSSE_NS, "UsernameToken");

    protected static final QName WSSE_PASSWORD = new QName(WSSE_NS, "Password");

    protected static final Set<QName> HEADERS = new HashSet<QName>();

    static {
        HEADERS.add(WSSE_SECURITY);
    }

    @Override
    public Set<QName> getHeaders() {
        return HEADERS;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean handleMessage(SOAPMessageContext context) {
        if ((Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            // we are only looking at inbound messages
            return true;
        }

        Map<String, String> callContextMap = null;

        Object[] secHeaders = context.getHeaders(WSSE_SECURITY, WSSE_CONTEXT, true);
        if (secHeaders != null && secHeaders.length > 0) {
            for (Object header : secHeaders) {
                if (!(header instanceof JAXBElement)) {
                    continue;
                }

                if (!(((JAXBElement<?>) header).getValue() instanceof SecurityHeaderType)) {
                    continue;
                }

                callContextMap = extractUsernamePassword((JAXBElement<SecurityHeaderType>) header);
                if (callContextMap != null) {
                    break;
                }
            }
        }

        // add user and password to context
        if (callContextMap == null) {
            callContextMap = new HashMap<String, String>();
        }

        context.put(AbstractService.CALL_CONTEXT_MAP, callContextMap);
        context.setScope(AbstractService.CALL_CONTEXT_MAP, MessageContext.Scope.APPLICATION);

        return true;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> extractUsernamePassword(JAXBElement<SecurityHeaderType> sht) {
        String username = null;
        String password = null;
        for (Object uno : sht.getValue().getAny()) {
            if ((uno instanceof JAXBElement) && ((JAXBElement<?>) uno).getValue() instanceof UsernameTokenType) {
                UsernameTokenType utt = ((JAXBElement<UsernameTokenType>) uno).getValue();
                username = utt.getUsername().getValue();

                for (Object po : utt.getAny()) {
                    if ((po instanceof JAXBElement) && ((JAXBElement<?>) po).getValue() instanceof PasswordString) {
                        password = ((JAXBElement<PasswordString>) po).getValue().getValue();
                        break;
                    }
                }

                break;
            }
        }
        Map<String, String> result = null;
        if (username != null) {
            result = new HashMap<String, String>();
            result.put(CallContext.USERNAME, username);
            result.put(CallContext.PASSWORD, password);
        }
        return result;
    }

    // --- JAXB classes ---

    @XmlRegistry
    public static class ObjectFactory {

        public ObjectFactory() {
        }

        public SecurityHeaderType createSecurityHeaderType() {
            return new SecurityHeaderType();
        }

        public UsernameTokenType createUsernameTokenType() {
            return new UsernameTokenType();
        }

        public PasswordString createPasswordString() {
            return new PasswordString();
        }

        public AttributedString createAttributedString() {
            return new AttributedString();
        }

        @XmlElementDecl(namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", name = "Security")
        public JAXBElement<SecurityHeaderType> createSecurity(SecurityHeaderType value) {
            return new JAXBElement<SecurityHeaderType>(WSSE_SECURITY, SecurityHeaderType.class, null, value);
        }

        @XmlElementDecl(namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", name = "UsernameToken")
        public JAXBElement<UsernameTokenType> createUsernameToken(UsernameTokenType value) {
            return new JAXBElement<UsernameTokenType>(WSSE_USERNAME_TOKEN, UsernameTokenType.class, null, value);
        }

        @XmlElementDecl(namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", name = "Password")
        public JAXBElement<PasswordString> createPassword(PasswordString value) {
            return new JAXBElement<PasswordString>(WSSE_PASSWORD, PasswordString.class, null, value);
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "SecurityHeaderType", propOrder = { "any" })
    public static class SecurityHeaderType {

        @XmlAnyElement(lax = true)
        protected List<Object> any;
        @XmlAnyAttribute
        private final Map<QName, String> otherAttributes = new HashMap<QName, String>();

        public List<Object> getAny() {
            if (any == null) {
                any = new ArrayList<Object>();
            }
            return this.any;
        }

        public Map<QName, String> getOtherAttributes() {
            return otherAttributes;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "UsernameTokenType", propOrder = { "username", "any" })
    public static class UsernameTokenType {

        @XmlElement(name = "Username", namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", required = true)
        protected AttributedString username;
        @XmlAnyElement(lax = true)
        protected List<Object> any;
        @XmlAttribute(name = "Id", namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd")
        @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
        @XmlID
        @XmlSchemaType(name = "ID")
        protected String id;
        @XmlAnyAttribute
        private final Map<QName, String> otherAttributes = new HashMap<QName, String>();

        public AttributedString getUsername() {
            return username;
        }

        public void setUsername(AttributedString value) {
            this.username = value;
        }

        public List<Object> getAny() {
            if (any == null) {
                any = new ArrayList<Object>();
            }
            return this.any;
        }

        public String getId() {
            return id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public Map<QName, String> getOtherAttributes() {
            return otherAttributes;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "PasswordString")
    public static class PasswordString extends AttributedString {

        @XmlAttribute(name = "Type")
        @XmlSchemaType(name = "anyURI")
        protected String type;

        public String getType() {
            return type;
        }

        public void setType(String value) {
            this.type = value;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "AttributedString", propOrder = { "value" })
    @XmlSeeAlso({ PasswordString.class })
    public static class AttributedString {

        @XmlValue
        protected String value;
        @XmlAttribute(name = "Id", namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd")
        @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
        @XmlID
        @XmlSchemaType(name = "ID")
        protected String id;
        @XmlAnyAttribute
        private final Map<QName, String> otherAttributes = new HashMap<QName, String>();

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public void setId(String value) {
            this.id = value;
        }

        public Map<QName, String> getOtherAttributes() {
            return otherAttributes;
        }
    }

}
