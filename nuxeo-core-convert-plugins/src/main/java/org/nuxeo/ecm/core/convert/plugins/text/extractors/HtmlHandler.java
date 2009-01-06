/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


public class HtmlHandler extends Xml2TextHandler {

    public HtmlHandler() throws SAXException, ParserConfigurationException {
        super();
    }

    public String getText() {
        return filterAndJoin(buf.toString());
    }
    private String filterAndJoin(String text) {
        boolean space = false;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if ((c == '\n') || (c == ' ') || Character.isWhitespace(c)) {
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


}
