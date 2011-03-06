/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.html;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.html.Utils;

public class TestUtils extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
    }

    public void testAddWebLengths() {
        assertEquals("30px", Utils.addWebLengths("10px", "20px"));
        assertEquals("3em", Utils.addWebLengths("1em", "2em"));
        assertNull(Utils.addWebLengths("25%", "2em"));
    }

    public void testSubstractWebLengths() {
        assertEquals("30px", Utils.substractWebLengths("40px", "10px"));
        assertEquals("3em", Utils.substractWebLengths("5em", "2em"));
        assertNull(Utils.substractWebLengths("25%", "2em"));
    }

    public void testDivideWebLengths() {
        assertEquals("20px", Utils.divideWebLength("40px", 2));
        assertEquals("1em", Utils.divideWebLength("5em", 3));
        assertNull(Utils.divideWebLength("25%", 0));
    }

    public void testGetMimeType() {
        assertEquals("image/png", Utils.getImageMimeType("png"));
        assertEquals("image/gif", Utils.getImageMimeType("gif"));
        assertEquals("image/jpeg", Utils.getImageMimeType("jpeg"));
        assertEquals("image/jpeg", Utils.getImageMimeType("jpg"));

        assertEquals("image/png", Utils.getImageMimeType("PNG"));
        assertEquals("image/gif", Utils.getImageMimeType("GIF"));
        assertEquals("image/jpeg", Utils.getImageMimeType("JPEG"));
        assertEquals("image/jpeg", Utils.getImageMimeType("JPG"));

        assertEquals("application/octet-stream",
                Utils.getImageMimeType("unknown"));
    }
}
