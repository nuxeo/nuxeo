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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.AbstractSAXParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.cyberneko.html.HTMLConfiguration;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class HtmlParser extends AbstractSAXParser {

    private static final Log log = LogFactory.getLog(HtmlParser.class);

    private StringBuffer buffer;

    public HtmlParser() {
        super(new HTMLConfiguration());
        try {
            // make sure we do not download the DTD URI
            setFeature("http://xml.org/sax/features/validation", false);
            setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
        } catch (SAXException e) {
            log.debug("Could not switch parser to non-validating: " +
                    e.getMessage());
        }
    }

    @Override
    public void startDocument(XMLLocator arg0, String arg1,
            NamespaceContext arg2, Augmentations arg3) throws XNIException {
        super.startDocument(arg0, arg1, arg2, arg3);
        buffer = new StringBuffer();
    }

    @Override
    public void characters(XMLString xmlString, Augmentations augmentations)
            throws XNIException {

        super.characters(xmlString, augmentations);

        buffer.append(xmlString.toString());
    }

    private String filterAndJoin(String text) {
        boolean space = false;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n' || c == ' ' || Character.isWhitespace(c)) {
                if (space) {
                    continue;
                } else {
                    space = true;
                    buffer.append(' ');
                    continue;
                }
            } else {
                if (!Character.isLetter(c) && !Character.isDigit(c)) {
                    if (!space) {
                        space = true;
                        buffer.append(' ');
                        continue;
                    }
                    continue;
                }
            }
            space = false;
            buffer.append(c);
        }
        return buffer.toString();
    }

    /**
     * @return the parsed content (as a String).
     */
    public String getContents() {
        return filterAndJoin(buffer.toString());
    }

}
