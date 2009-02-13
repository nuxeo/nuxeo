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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OOoXmlContentHandler extends DefaultHandler {

    private StringBuffer sb;
    private boolean dumpText;

    public OOoXmlContentHandler() {
        sb = new StringBuffer();
        dumpText = false;
    }

    public String getContent() {
        return sb.toString();
    }

    @Override
    public void startElement(String namespaceURI, String localName,
            String rawName, Attributes atts)
            throws SAXException {
        if (rawName.startsWith("text:")) {
            dumpText = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (dumpText) {
            sb.append(ch, start, length);
            sb.append(" ");
        }
    }
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        dumpText = false;
    }

}
