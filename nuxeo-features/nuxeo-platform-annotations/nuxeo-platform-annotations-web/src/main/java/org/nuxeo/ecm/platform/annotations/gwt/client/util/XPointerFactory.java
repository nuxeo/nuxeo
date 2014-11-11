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

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class XPointerFactory {

    private static final String IMAGE_RANGE = "image-range";

    private static final String STRING_RANGE = "string-range";

    private static final String NULL_RANGE = "null-range";

    private XPointerFactory() {
    }

    public static XPointer getXPointer(String xpointer) {
        if (xpointer.contains(STRING_RANGE)) {
            return new StringRangeXPointer(xpointer);
        } else if (xpointer.contains(IMAGE_RANGE)) {
            return new ImageRangeXPointer(xpointer);
        } else if (xpointer.contains(NULL_RANGE)) {
            return new NullRangeXPointer(xpointer);
        }
        return null;
    }

    public static boolean isStringRange(String xpointer) {
        return xpointer.contains(STRING_RANGE);
    }

    public static boolean isImageRange(String xpointer) {
        return xpointer.contains(IMAGE_RANGE);
    }

}
