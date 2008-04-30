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

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deployContrib("OSGI-INF/site-template-framework.xml");
        deployContrib("OSGI-INF/test-ftl-templates-contrib.xml");
        deployContrib("OSGI-INF/test-site-adapters-service-contrib.xml");

        CoreSession session = getCoreSession();

        DocumentModel workspaces = session.getDocument(new PathRef("/default-domain/workspaces"));

        DocumentModel site = session.createDocumentModel(workspaces.getPathAsString(), "testSite",
                "Folder");
        site.setProperty("dublincore", "title", "TestSite");
        site = session.createDocument(site);

        page = session.createDocumentModel(site.getPathAsString(), "testPage", "Note");
        page.setProperty("dublincore", "title", "TestPage");
        page.setProperty("note", "note", "${title}");
        page = session.createDocument(page);

        DocumentModel file = session.createDocumentModel(site.getPathAsString(), "testFile",
                "File");
        file.setProperty("dublincore", "title", "TestPage");
        file = session.createDocument(file);

        session.save();
    }

    public void testUpdateTemplate() throws Exception {
        FakeResponse response = execSiteRequest("GET", "/testPage");
        assertEquals(200, response.getStatus());
        String output = response.getOutput();
        assertEquals("TestPage", output);

        response = execSiteRequest("POST", "/testPage");
        String postOutput = response.getOutput();
        System.out.println(postOutput);
        assertEquals(420, response.getStatus());
        assertTrue(postOutput.startsWith("Unable to update"));

        response = execSiteRequest("POST", "/testPage?note=new${title}");
        postOutput = response.getOutput();
        System.out.println(postOutput);
        assertEquals(200, response.getStatus());
        DocumentModel note = getCoreSession().getDocument(page.getRef());
        String noteContent = (String) note.getProperty("note", "note");
        assertEquals("new${title}", noteContent);
        assertEquals("newTestPage", postOutput);
    }

}
