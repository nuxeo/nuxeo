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

package org.nuxeo.ecm.webengine.tests.wiki;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.tests.BaseSiteRequestTestCase;
import org.nuxeo.ecm.webengine.tests.fake.FakeResponse;

public class TestWikiRequests extends BaseSiteRequestTestCase {

    DocumentModel page;
    DocumentModel site;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deployContrib("OSGI-INF/site-template-framework.xml");
        deployContrib("OSGI-INF/test-site-adapters-service-contrib.xml");
        deployContrib("OSGI-INF/wiki-fake-adapters-contrib.xml");
        deployContrib("OSGI-INF/wiki-fake-template-contrib.xml");

        CoreSession session = getCoreSession();

        DocumentModel workspaces = session.getDocument(new PathRef(
                "/default-domain/workspaces"));

        site = session.createDocumentModel(
                workspaces.getPathAsString(), "testSite", "Folder");
        site.setProperty("dublincore", "title", "TestSite");
        site = session.createDocument(site);

        page = session.createDocumentModel(site.getPathAsString(), "testPage",
                "Note");
        page.setProperty("dublincore", "title", "TestPage");
        page.setProperty("note", "note", "${title}");
        page = session.createDocument(page);

        session.save();
    }

    public void testCreateNewPage() throws Exception {

        // Verify page creation prompt
        FakeResponse response = execSiteRequest("GET", "/testSite/testNewPage");
        assertEquals(200, response.getStatus());
        String output = response.getOutput();
        System.out.println(output);
        assertTrue(output.contains("CreatePage"));

        // Verify page creation
        response = execSiteRequest("GET", "/testSite/testNewPage?create=true");
        assertEquals(200, response.getStatus());
        output = response.getOutput();
        System.out.println(output);
        assertTrue(output.contains("Edit Wiki Page"));
        DocumentModel newPage = getCoreSession().getDocument(new PathRef(site.getPathAsString() + "/testNewPage"));

        assertNotNull(newPage);
        assertEquals("testNewPage", newPage.getTitle());
        assertTrue(((String)newPage.getProperty("note", "note")).contains("<h2>${this.title}</h2>Use the Edit link to edit this newly created page."));

        // Verify page rendering
        response = execSiteRequest("GET", "/testSite/testNewPage");
        assertEquals(200, response.getStatus());
        output = response.getOutput();
        System.out.println(output);
        assertTrue(output.contains("<h2>testNewPage</h2>"));

        // Verify page update
        response = execSiteRequest("POST", "/testSite/testNewPage?note=updated-${title}");
        assertEquals(200, response.getStatus());
        output = response.getOutput();
        System.out.println(output);
        assertTrue(output.contains("updated-testNewPage"));
    }

}
