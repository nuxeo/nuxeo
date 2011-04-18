/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.webdav;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simple test using the Jersey HTTP client.
 * Only standard HTTP methods are supported, so we're only testing GET, PUT and DELETE.
 */
@Ignore
public class JerseyClientTest extends AbstractServerTest {

    @Test
    public void simpleTest() {
        Client client = Client.create();
        WebResource r = client.resource(ROOT_URI);

        String e1 = r.path("").get(String.class);
        assertTrue(e1.length() > 0);

        // Create / remove file

        r.path("file").put("some content");
        String e2 = r.path("file").get(String.class);
        assertEquals("some content", e2);

        r.path("file").put("different content");
        String e3 = r.path("file").get(String.class);
        assertEquals("different content", e3);

        r.path("file").delete();
        try {
            String e4 = r.path("file").get(String.class);
            fail("Should have raise a 'doc not found' exception");
        } catch (Exception e) {
            // OK.
        }
    }

    @Test
    public void multipleTest() {
        for (int i = 0; i < 10; i++) {
            simpleTest();
        }
    }

}
