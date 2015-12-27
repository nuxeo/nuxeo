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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class ImageRangeXPointer implements XPointer {

    private final String url;

    private final String path;

    private final Point topLeft;

    private final Point bottomRight;

    private String xpointerString;

    public ImageRangeXPointer(String xpointer) {
        xpointerString = xpointer;
        this.url = xpointer.substring(0, xpointer.indexOf("#"));
        xpointer = xpointer.replaceFirst(".*image-range\\(", "");
        xpointer = xpointer.replaceFirst("\\)\\)$", "");
        String[] args = xpointer.split(",");
        this.path = args[0];
        this.topLeft = new Point(args[1] + "," + args[2]);
        this.bottomRight = new Point(args[3] + "," + args[4]);
    }

    public String getMethod() {
        return "image-range";
    }

    public String getUrl() {
        return url;
    }

    public String getXPath() {
        return path;
    }

    public ImageElement getImage() {
        return getImage(false);
    }

    public ImageElement getImage(boolean multiImage) {
        Document document = Document.get();
        if (!multiImage) {
            String idablePath = XPathUtil.toIdableName(path);
            Element div = document.getElementById(idablePath);
            if (div == null) {
                return null;
            }
            return (ImageElement) div.getFirstChild();
        } else {
            return (ImageElement) document.getElementById("annotationRootImage");
        }
    }

    public Point getTopLeft() {
        return topLeft;
    }

    public Point getBottomRight() {
        return bottomRight;
    }

    public String getXpointerString() {
        return xpointerString;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImageRangeXPointer)) {
            return false;
        }
        ImageRangeXPointer xp = (ImageRangeXPointer) obj;
        return xpointerString.equals(xp.xpointerString);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 17 * xpointerString.hashCode();
        return result;
    }

}
