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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OOoXmlContentHandler extends DefaultHandler {

    protected static final Log log = LogFactory.getLog(OOoXmlContentHandler.class);

    protected final StringBuffer sb = new StringBuffer();

    protected final Stack<String> path = new Stack<String>();

    protected boolean dumpText = false;

    protected boolean isSpreadSheet = false;

    public String getContent() {
        return sb.toString();
    }

    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {
        path.push(qName);

        // Detect spreadsheet
        if (qName.equals("office:spreadsheet")) {
            isSpreadSheet = true;
        }

        // Text element
        if (qName.startsWith("text:")) {
            dumpText = true;
        }
        // Heading (Writer only): add a new line.
        // If the heading's outline level is > 1 (not the document title), add
        // an extra new line.
        if (qName.equals("text:h")) {
            sb.append("\n");
            String outlineLevelAtt = atts.getValue("text:outline-level");
            if (!StringUtils.isEmpty(outlineLevelAtt)) {
                int outlineLevel = -1;
                try {
                    outlineLevel = Integer.parseInt(outlineLevelAtt);
                } catch (NumberFormatException nfe) {
                    log.warn("Attribute 'text:outline-level' on element 'text:h' has a non integer value.");
                }
                if (outlineLevel > 1) {
                    sb.append("\n");
                }
            }
        }
        // Paragraph: add a new line
        if (!isSpreadSheet && qName.equals("text:p")) {
            sb.append("\n");
        }

        // Page (Impress only): add a new line if not the first one
        if (qName.equals("draw:page")
                && !"page1".equals(atts.getValue("draw:name"))) {
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
        if (path.isEmpty() || !path.lastElement().startsWith("text:")) {
            dumpText = false;
        }

        // Specific separators for spreadsheets:
        if (isSpreadSheet) {

            // End of table row: add a blank line
            if (qName.equals("table:table-row")) {
                sb.append("\n\n");
            }

            // End of table cell: add a separator
            if (qName.equals("table:table-cell")) {
                sb.append(" ");
            }

            // End of paragraph: add a white space
            if (qName.equals("text:p")) {
                sb.append(" ");
            }
        }
    }

}
