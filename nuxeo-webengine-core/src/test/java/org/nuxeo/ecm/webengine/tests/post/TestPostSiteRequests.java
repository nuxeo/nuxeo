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
 * $Id$
 */

package org.nuxeo.ecm.webengine.tests.post;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.tests.BaseSiteRequestTestCase;
import org.nuxeo.ecm.webengine.tests.fake.FakeResponse;

public class TestPostSiteRequests extends BaseSiteRequestTestCase {

    DocumentModel page;

    public void testUpdateTemplate() throws Exception {
        FakeResponse response = execSiteRequest("GET", "/site/page");
        assertEquals(200, response.getStatus());
        String output = response.getOutput();
        //TODO here we may register special test pages to be able to compare output
        //assertEquals("Page", output);
        assertTrue(output.length() > 0);

        response = execSiteRequest("POST", "/site/page@@update");
        String postOutput = response.getOutput();
        //System.out.println(postOutput);
        assertEquals(200, response.getStatus());
        CoreSession coreSession = getNewSession();
        DocumentModel doc = getCoreSession().getDocument(new PathRef("/site/page"));
        assertEquals("Content", doc.getProperty("note", "note"));
        coreSession.destroy();

        response = execSiteRequest("POST", "/site/page@@update?note:note=new${This.title}");
        postOutput = response.getOutput();
        System.out.println(postOutput);
        assertEquals(200, response.getStatus());
        coreSession = getNewSession();
        doc = getCoreSession().getDocument(new PathRef("/site/page"));
        assertEquals("new${This.title}", doc.getProperty("note", "note"));
        coreSession.destroy();

    }

}
