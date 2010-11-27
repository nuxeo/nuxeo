/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OOoXmlContentHandler extends DefaultHandler {

    protected final StringBuffer sb = new StringBuffer();

    protected final Stack<String> path = new Stack<String>();

    protected boolean dumpText = false;

    public String getContent() {
        return sb.toString();
    }

    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts)
            throws SAXException {
        path.push(qName);
        if (qName.startsWith("text:")) {
            dumpText = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (dumpText) {
            String content = String.valueOf(ch, start, length);
            sb.append(content);
            sb.append(" ");
        }
    }
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        path.pop();
        if (path.isEmpty() || !path.lastElement().startsWith("text:")) {
            dumpText = false;
        }
    }

}
