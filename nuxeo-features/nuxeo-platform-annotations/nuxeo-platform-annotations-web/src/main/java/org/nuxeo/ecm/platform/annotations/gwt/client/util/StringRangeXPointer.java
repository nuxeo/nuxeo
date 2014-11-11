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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class StringRangeXPointer implements XPointer {
    private final XPathUtil pathUtil = new XPathUtil();

    private final String url;

    private final String path;

    private final int startOffset;

    private final int length;

    private String xpointerString;

    public StringRangeXPointer(String xpointer) {
        xpointerString = xpointer;
        this.url = xpointer.substring(0, xpointer.indexOf("#"));
        xpointer = xpointer.replaceFirst(".*string-range\\(", "");
        xpointer = xpointer.replaceFirst("\\)\\)$", "");
        String[] args = xpointer.split(",");
        this.path = args[0];
        this.startOffset = Integer.parseInt(args[2].trim());
        this.length = Integer.parseInt(args[3].trim());
    }

    public int getLength() {
        return length;
    }

    public String getMethod() {
        return "string-range";
    }

    public String getUrl() {
        return url;
    }

    public String getXPath() {
        return path;
    }

    public Node getFirstNode() {
        return pathUtil.getNode(path,
                Document.get()).get(
                0);
    }

    public int getStartOffset() {
        return startOffset;
    }

    public Document getOwnerDocument() {
        return Document.get();
    }

    public String getXpointerString() {
        return xpointerString;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StringRangeXPointer)) {
            return false;
        }
        StringRangeXPointer xp = (StringRangeXPointer) obj;
        return xpointerString.equals(xp.xpointerString);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 17 * xpointerString.hashCode();
        return result;
    }

}
