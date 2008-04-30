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

package org.nuxeo.ecm.webengine.tests.get;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.tests.BaseSiteRequestTestCase;
import org.nuxeo.ecm.webengine.tests.fake.FakeResponse;

public class TestGetSiteRequests extends BaseSiteRequestTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CoreSession session = getCoreSession();

        DocumentModel workspaces = session.getDocument(new PathRef("/default-domain/workspaces"));

        DocumentModel site = session.createDocumentModel(workspaces.getPathAsString(), "testSite",
                "Folder");
        site.setProperty("dublincore", "title", "TestSite");
        site = session.createDocument(site);

        DocumentModel page = session.createDocumentModel(site.getPathAsString(), "testPage",
                "Note");
        page.setProperty("dublincore", "title", "TestPage");
        page.setProperty("note", "note", "Content");
        page = session.createDocument(page);

        session.save();
    }

    public void testResolver() throws Exception {
        FakeResponse response = execSiteRequest("GET", "/testSite/testPage");
        assertEquals(200, response.getStatus());

        FakeResponse response2 = execSiteRequest("GET", "/testSite/testPage2");
        assertEquals(404, response2.getStatus());
    }


}
