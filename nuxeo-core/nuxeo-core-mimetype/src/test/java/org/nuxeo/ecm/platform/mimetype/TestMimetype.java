/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
