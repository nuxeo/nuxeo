/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OpenXmlContentHandler extends DefaultHandler {

    protected final StringBuffer sb = new StringBuffer();

    protected final Stack<String> path = new Stack<String>();

    protected boolean dumpText = false;

    public String getContent() {
        return sb.toString();
    }

    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {
        path.push(qName);
        // Text element of a docx or pptx document
        if (qName.equals("w:t") || qName.equals("a:t")) {
            dumpText = true;
        }
        // If the paragraph's style is "styleX" with X > 1 (this is a heading,
        // but not the document title), add a new line.
        if (qName.equals("w:pStyle")
                && !"style0".equals(atts.getValue("w:val"))
                && !"style1".equals(atts.getValue("w:val"))) {
            sb.append("\n");

        }
        // Paragraph: add a new line
        if (qName.equals("w:p") || qName.equals("a:p")) {
            sb.append("\n");
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (dumpText) {
            String content = String.valueOf(ch, start, length);
            sb.append(content);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        path.pop();
        if (path.isEmpty() || !path.lastElement().equals("w:t")) {
            dumpText = false;
        }
    }

}
