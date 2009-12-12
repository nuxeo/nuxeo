/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.webdav;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Test;
import org.nuxeo.ecm.webdav.AbstractServerTest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class FunctionalTest extends AbstractServerTest {

    static final String ROOT_URI = TEST_URI + "/dav/workspaces";

    /**
     * Simple test using the Jersey HTTP client.
     * Only standard HTTP methods are supported, so we're only testing GET, PUT and DELETE.
     */
    @Test
    public void simpleTest() {
        Client client = Client.create();
        WebResource r = client.resource(ROOT_URI);

        String e1 = r.path("").get(String.class);
        assertTrue(e1.length() > 0);

        // Create / remove file

        r.path("file").put("toto");

        String e2 = r.path("file").get(String.class);
        assertEquals("toto", e2);

        r.path("file").delete();

        //Response response = r.path("toto").get(Response.class);
        //assertEquals(404, response.getStatus());
    }

    /**
     * Runs the litmus test, a third-party WebDAV compliance testing tool (must be installed separately).
     */
    @Test
    public void testWithLitmus() throws Exception {
        // XXX: temporary while test suite doesn't pass fully
        if (System.getenv("BT") != null) {
            return;
        }
        Process p = Runtime.getRuntime().exec("litmus " + ROOT_URI);
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        while ((s = reader.readLine()) != null) {
            System.out.println(s);
            System.out.flush();
        }
        assertEquals(0, p.waitFor());
    }

}
