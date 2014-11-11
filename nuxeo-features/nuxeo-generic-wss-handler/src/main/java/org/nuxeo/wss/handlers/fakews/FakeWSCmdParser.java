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

package org.nuxeo.wss.handlers.fakews;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.nuxeo.wss.servlet.WSSRequest;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class FakeWSCmdParser extends DefaultHandler {

    protected String paramNameTag = "pageUrl";

    protected static FakeWSCmdParser instance;

    protected boolean readString = false;

    public String paramValue;

    public FakeWSCmdParser(String paramNameTag) {
        this.paramNameTag = paramNameTag;
    }

    @Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
        if (paramNameTag.equalsIgnoreCase(name)) {
            readString = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        if (paramNameTag.equalsIgnoreCase(name)) {
            readString = false;
        }
    }


    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (readString) {
            if (paramValue == null) {
                paramValue = String.valueOf(ch, start, length);
            } else {
                paramValue = paramValue + String.valueOf(ch, start, length);
            }
        }
    }

    public String getParameter(WSSRequest request) throws Exception {
        InputStream in = null;

        BufferedReader httpReader = request.getHttpRequest().getReader();
        if (httpReader == null) {
            // for unit tests
            in = request.getHttpRequest().getInputStream();
        } else {
            StringBuffer sb = new StringBuffer();
            String line = httpReader.readLine();
            while (line != null) {
                sb.append(line);
                line = httpReader.readLine();
            }
            in = new ByteArrayInputStream(sb.toString().getBytes());
        }

        XMLReader reader;
        reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(this);
        reader.setFeature("http://xml.org/sax/features/namespaces", false);
        reader.setFeature("http://xml.org/sax/features/validation", false);
        reader.parse(new InputSource(in));
        return paramValue;
    }

}
