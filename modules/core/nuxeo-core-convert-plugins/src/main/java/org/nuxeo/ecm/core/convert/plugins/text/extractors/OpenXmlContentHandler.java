/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OpenXmlContentHandler extends DefaultHandler {

    protected final StringBuilder sb = new StringBuilder();

    protected final Stack<String> path = new Stack<>();

    protected boolean dumpText = false;

    public String getContent() {
        return sb.toString();
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        path.push(qName);
        // Text element of a docx or pptx document
        if (qName.equals("w:t") || qName.equals("a:t")) {
            dumpText = true;
        }
        // If the paragraph's style is "styleX" with X > 1 (this is a heading,
        // but not the document title), add a new line.
        if (qName.equals("w:pStyle") && !"style0".equals(atts.getValue("w:val"))
                && !"style1".equals(atts.getValue("w:val"))) {
            sb.append("\n");

        }
        // Paragraph: add a new line
        if (qName.equals("w:p") || qName.equals("a:p")) {
            sb.append("\n");
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (dumpText) {
            String content = String.valueOf(ch, start, length);
            sb.append(content);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        path.pop();
        if (path.isEmpty() || !path.lastElement().equals("w:t")) {
            dumpText = false;
        }
    }

}
