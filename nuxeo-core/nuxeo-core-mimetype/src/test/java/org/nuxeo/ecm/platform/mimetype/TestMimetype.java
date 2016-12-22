/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestMimetype.java 28989 2008-01-12 23:08:51Z sfermigier $
 */

package org.nuxeo.ecm.platform.mimetype;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;

/**
 * Test the mimetype class behavior.
 *
 * @author <a href="ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestMimetype {

    private MimetypeEntry mimetype;

    @Before
    public void setUp() {
        String normalizedMimetype = MimetypeRegistry.PDF_MIMETYPE;

        List<String> mimetypes = new ArrayList<String>();
        mimetypes.add(MimetypeRegistry.PDF_MIMETYPE);
        // fake
        mimetypes.add("app/whatever-pdf");

        List<String> extensions = new ArrayList<String>();
        extensions.add("pdf");
        extensions.add("fake");

        String iconPath = "pdf.png";

        boolean binary = true;
        boolean onlineEditable = false;
        boolean oleSupported = false;

        mimetype = new MimetypeEntryImpl(normalizedMimetype, mimetypes, extensions, iconPath, binary, onlineEditable,
                oleSupported);
    }

    @After
    public void tearDown() {
        mimetype = null;
    }

    @Test
    public void testBinary() {
        assertTrue(mimetype.isBinary());
    }

    @Test
    public void testOnlineEditable() {
        assertFalse(mimetype.isOnlineEditable());
    }

    @Test
    public void testOleSupported() {
        assertFalse(mimetype.isOleSupported());
    }

    @Test
    public void testExtensions() {
        List<String> extensions = mimetype.getExtensions();
        assertTrue(extensions.contains("pdf"));
        assertTrue(extensions.contains("fake"));
    }

    // @Test
    public void xtestIcon() {
        String iconPath = mimetype.getIconPath();
        assertNotNull(iconPath);
    }

    @Test
    public void testMimetypes() {
        assertEquals(MimetypeRegistry.PDF_MIMETYPE, mimetype.getNormalized());
        assertEquals("application", mimetype.getMajor());
        assertEquals("pdf", mimetype.getMinor());
        List<String> mimetypes = mimetype.getMimetypes();
        assertTrue(mimetypes.contains(MimetypeRegistry.PDF_MIMETYPE));
        assertTrue(mimetypes.contains("app/whatever-pdf"));
    }

    @Test
    public void testNormalized() {
        assertEquals(MimetypeRegistry.PDF_MIMETYPE, mimetype.getNormalized());
    }

}
