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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class GwtTestImageRangeXPointer extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "org.nuxeo.ecm.platform.annotations.gwt.AnnotationPanel";
    }

    public void testImageRangeXPointer() {
        ImageRangeXPointer xpointer = new ImageRangeXPointer(
                "http://serv1.example.com/some/page.html#xpointer(image-range(/html[1]/body[0]/img[0],[79,133],[123,159]))");
        assertNotNull(xpointer);
        assertEquals("http://serv1.example.com/some/page.html", xpointer.getUrl());
        assertEquals("/html[1]/body[0]/img[0]", xpointer.getXPath());
        assertEquals(79, xpointer.getTopLeft().getX());
        assertEquals(133, xpointer.getTopLeft().getY());
        assertEquals(123, xpointer.getBottomRight().getX());
        assertEquals(159, xpointer.getBottomRight().getY());
    }

}
