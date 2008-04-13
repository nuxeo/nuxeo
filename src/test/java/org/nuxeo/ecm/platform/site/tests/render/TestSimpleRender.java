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

package org.nuxeo.ecm.platform.site.tests.render;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.site.tests.BaseSiteRequestTestCase;
import org.nuxeo.ecm.platform.site.tests.fake.FakeResponse;

public class TestSimpleRender extends BaseSiteRequestTestCase {

    DocumentModel site=null;
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deployContrib("OSGI-INF/site-manager-framework.xml");
        deployContrib("OSGI-INF/test-ftl-templates-contrib.xml");
        deployContrib("OSGI-INF/test-site-adapters-service-contrib.xml");

        CoreSession session = getCoreSession();

        DocumentModel workspaces = session.getDocument(new PathRef("/default-domain/workspaces"));

        site = session.createDocumentModel(workspaces.getPathAsString(),"testSite", "Folder");
        site.setProperty("dublincore", "title", "TestSite");
        site = session.createDocument(site);

        DocumentModel page = session.createDocumentModel(site.getPathAsString(),"testPage", "Note");
        page.setProperty("dublincore", "title", "TestPage");
        page.setProperty("note", "note", "${title}");
        page = session.createDocument(page);

        DocumentModel file = session.createDocumentModel(site.getPathAsString(),"testFile", "File");
        file.setProperty("dublincore", "title", "TestPage");
        file = session.createDocument(file);

        session.save();
    }

    public void testStaticTemplate() throws Exception
    {
        FakeResponse response = execSiteRequest("GET", "/testSite/testFile");
        assertEquals(200,response.getStatus());

        String output = response.getOutput();

        System.out.println(output);
        assertTrue(output.contains("My Path: /default-domain/workspaces/testSite/testFile"));
        assertTrue(output.contains("My Super Context Doc Path: /default-domain/workspaces/testSite"));
    }

    public void testDynamicTemplate() throws Exception
    {
        FakeResponse response = execSiteRequest("GET", "/testPage");
        assertEquals(200,response.getStatus());

        String output = response.getOutput();

        assertEquals("TestPage", output);
        System.out.println(output);

    }

    public void testDynamicTemplateWithURLs() throws Exception
    {

        CoreSession session = getCoreSession();

        DocumentModel page2 = session.createDocumentModel(site.getPathAsString(),"testPage2", "Note");
        page2.setProperty("dublincore", "title", "TestPage2");
        page2.setProperty("note", "note", "document url=${docURL}");
        page2 = session.createDocument(page2);
        session.save();

        FakeResponse response = execSiteRequest("GET", "/testPage2");
        String output = response.getOutput();
        System.out.println(output);
        assertEquals(200,response.getStatus());
        assertEquals("document url=/nuxeo/site/testPage2", output);


        response = execSiteRequest("GET", "/testSite/testPage2");
        output = response.getOutput();
        System.out.println(output);
        assertEquals(200,response.getStatus());
        assertTrue(output.contains("document url=/nuxeo/site/testSite/testPage2"));


    }

    public void testDynamicTemplateWithRequestParameterAccess() throws Exception
    {

        CoreSession session = getCoreSession();

        DocumentModel page2 = session.createDocumentModel(site.getPathAsString(),"testPage2", "Note");
        page2.setProperty("dublincore", "title", "TestPage2");
        page2.setProperty("note", "note", "param1=${request.getParameter(\"param1\")}");
        page2 = session.createDocument(page2);
        session.save();

        FakeResponse response = execSiteRequest("GET", "/testPage2?param1=toto");
        String output = response.getOutput();
        System.out.println(output);
        assertEquals(200,response.getStatus());
        assertEquals("param1=toto", output);



    }

}
