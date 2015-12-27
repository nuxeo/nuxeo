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
