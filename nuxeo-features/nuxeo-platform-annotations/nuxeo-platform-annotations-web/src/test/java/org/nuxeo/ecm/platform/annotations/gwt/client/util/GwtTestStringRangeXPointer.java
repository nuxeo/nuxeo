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
 * @author Alexandre Russel
 *
 */
public class GwtTestStringRangeXPointer extends GWTTestCase {
    @Override
    public String getModuleName() {
        return "org.nuxeo.ecm.platform.annotations.gwt.AnnotationPanel";
    }

    public void testStringRangeXPointer() {
        StringRangeXPointer xpointer = new StringRangeXPointer(
                "http://localhost:8080/nuxeo/nxdoc/default/f6c3a8c3-427f-40fc-a0a0-e7630c41fdce/#xpointer(string-range(/HTML[1]/BODY[0]/DIV[0]/DIV[0]/NOBR[0]/SPAN[0],\"\",18,0))");
        assertNotNull(xpointer);
        assertEquals(xpointer.getLength(), 0);
        assertEquals(xpointer.getStartOffset(), 18);
        assertEquals(
                xpointer.getUrl(),
                "http://localhost:8080/nuxeo/nxdoc/default/f6c3a8c3-427f-40fc-a0a0-e7630c41fdce/");
        assertEquals(xpointer.getXPath(),
                "/HTML[1]/BODY[0]/DIV[0]/DIV[0]/NOBR[0]/SPAN[0]");
    }
}
