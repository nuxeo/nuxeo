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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
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
        assertEquals("http://serv1.example.com/some/page.html",
                xpointer.getUrl());
        assertEquals("/html[1]/body[0]/img[0]", xpointer.getXPath());
        assertEquals(79, xpointer.getTopLeft().getX());
        assertEquals(133, xpointer.getTopLeft().getY());
        assertEquals(123, xpointer.getBottomRight().getX());
        assertEquals(159, xpointer.getBottomRight().getY());
    }

}
