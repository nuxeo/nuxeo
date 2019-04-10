/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.wss.fprpc;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * SAX ContentHandler to parse CAML Batch requests.
 * <p>
 * References : - http://msdn.microsoft.com/en-us/library/dd586422(office.11).aspx
 *
 * @author Thierry Delprat
 */
public class CAMLHandler extends DefaultHandler {

    protected List<FPRPCCall> calls = new ArrayList<FPRPCCall>();

    protected FPRPCCall currentCall = null;

    protected String currentParameterName = null;

    protected String currentParameterValue = null;

    public static final String METHOD_TAG = "Method";

    public static final String SETVAR_TAG = "SetVar";

    public static final String ID_ATTRIBUTE = "ID";

    public static final String NAME_ATTRIBUTE = "Name";

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if (METHOD_TAG.equalsIgnoreCase(name)) {
            currentCall = new FPRPCCall();
            String id = attributes.getValue(ID_ATTRIBUTE);
            currentCall.setId(id);
        } else if (SETVAR_TAG.equalsIgnoreCase(name)) {
            currentParameterName = attributes.getValue(NAME_ATTRIBUTE);
            currentParameterValue = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (currentParameterName != null) {
            if (currentParameterValue == null) {
                currentParameterValue = String.valueOf(ch, start, length);
            } else {
                currentParameterValue = currentParameterValue + String.valueOf(ch, start, length);
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        if (METHOD_TAG.equalsIgnoreCase(name)) {
            calls.add(currentCall);
            currentCall = null;
        } else if (SETVAR_TAG.equalsIgnoreCase(name)) {
            if (FPRPCConts.CMD_PARAM.equalsIgnoreCase(currentParameterName)) {
                currentCall.setMethodName(currentParameterValue);
            } else {
                currentCall.addParameter(currentParameterName, currentParameterValue);
            }
            currentParameterName = null;
            currentParameterValue = null;
        }
    }

    public List<FPRPCCall> getParsedCalls() {
        return calls;
    }

    public static XMLReader getXMLReader() throws SAXException {
        XMLReader reader;
        reader = XMLReaderFactory.createXMLReader();
        CAMLHandler handler = new CAMLHandler();
        reader.setContentHandler(handler);
        reader.setFeature("http://xml.org/sax/features/namespaces", false);
        reader.setFeature("http://xml.org/sax/features/validation", false);
        reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return reader;
    }

}
