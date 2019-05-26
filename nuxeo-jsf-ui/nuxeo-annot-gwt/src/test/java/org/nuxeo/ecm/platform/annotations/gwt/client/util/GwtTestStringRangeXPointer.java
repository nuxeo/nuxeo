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
 * @author Alexandre Russel
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
        assertEquals(xpointer.getUrl(),
                "http://localhost:8080/nuxeo/nxdoc/default/f6c3a8c3-427f-40fc-a0a0-e7630c41fdce/");
        assertEquals(xpointer.getXPath(), "/HTML[1]/BODY[0]/DIV[0]/DIV[0]/NOBR[0]/SPAN[0]");
    }
}
