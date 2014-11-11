/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Xml2TextHandler extends DefaultHandler {

    protected static final SAXParserFactory factory = SAXParserFactory.newInstance();

    static {
        factory.setValidating(false);
        factory.setNamespaceAware(false);
    }

    protected SAXParser parser;
    protected StringBuffer buf;
    protected boolean trim = false;

    public Xml2TextHandler() throws SAXException, ParserConfigurationException {
        parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setFeature("http://xml.org/sax/features/validation", false);
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }

    public SAXParser getParser() {
        return parser;
    }

    public String parse(File file) throws SAXException, IOException {
        parser.parse(file, this);
        String text = buf.toString();
        buf = null;
        return text;
    }

    public String parse(InputStream in) throws SAXException, IOException {
        parser.parse(in, this);
        String text = buf.toString();
        buf = null;
        return text;
    }

    public String parse(InputSource is) throws SAXException, IOException {
        parser.parse(is, this);
        String text = buf.toString();
        buf = null;
        return text;
    }

    public String getText() {
        return buf.toString();
    }

    @Override
    public void startDocument() throws SAXException {
        trim = false;
        buf = new StringBuffer();
    }

    @Override
    public void startElement(String uri, String localName,
            String name, Attributes attributes) throws SAXException {
        trim = true;
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        trim = true;
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        //buf.append(ch, start, length); if (true) return;
        if (trim) {
            int i = start;
            int end = start + length;
            while (i < end && Character.isWhitespace(ch[i])) {
                i++;
            }
            buf.append(" ").append(ch, i, length - i + start);
            trim = false;
            //System.out.println("["+new String(ch, i, length - i + start)+"]");
        } else {
            buf.append(ch, start, length);
            //System.out.println("{"+new String(ch, start, length)+"}");
        }
    }

}
