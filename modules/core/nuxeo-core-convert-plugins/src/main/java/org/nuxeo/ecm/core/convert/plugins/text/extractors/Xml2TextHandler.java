/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    protected StringBuilder builder;

    protected boolean trim = false;

    public Xml2TextHandler() throws SAXException, ParserConfigurationException {
        parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setFeature("http://xml.org/sax/features/validation", false);
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    }

    public SAXParser getParser() {
        return parser;
    }

    public String parse(File file) throws SAXException, IOException {
        parser.parse(file, this);
        String text = builder.toString();
        builder = null;
        return text;
    }

    public String parse(InputStream in) throws SAXException, IOException {
        parser.parse(in, this);
        String text = builder.toString();
        builder = null;
        return text;
    }

    public String parse(InputSource is) throws SAXException, IOException {
        parser.parse(is, this);
        String text = builder.toString();
        builder = null;
        return text;
    }

    public String getText() {
        return builder.toString();
    }

    @Override
    public void startDocument() throws SAXException {
        trim = false;
        builder = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        trim = true;
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        trim = true;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // sb.append(ch, start, length); if (true) return;
        if (trim) {
            int i = start;
            int end = start + length;
            while (i < end && Character.isWhitespace(ch[i])) {
                i++;
            }
            builder.append(" ").append(ch, i, length - i + start);
            trim = false;
            // System.out.println("["+new String(ch, i, length - i + start)+"]");
        } else {
            builder.append(ch, start, length);
            // System.out.println("{"+new String(ch, start, length)+"}");
        }
    }

}
