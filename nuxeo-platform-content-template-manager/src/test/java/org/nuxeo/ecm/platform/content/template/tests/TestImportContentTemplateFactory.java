/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.content.template.tests;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.content.template.service.ContentTemplateService;
import org.nuxeo.runtime.api.Framework;

public class TestImportContentTemplateFactory extends SQLRepositoryTestCase {

    @Test
    public void testNothing() {
        assertNull(null);
    }


    protected ContentTemplateService service;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests",
                "OSGI-INF/test-import-data-mock-type-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.content.template.tests",
                "OSGI-INF/test-import-data-workspace-contentTemplate-contrib.xml");
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testData1ImportFactory() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.content.template.tests",
                "OSGI-INF/test-import-data1-content-template-contrib.xml");
        openSession();
        DocumentModel root = session.getDocument(new PathRef("/"));
        assertNotNull(root);
        service = Framework.getLocalService(ContentTemplateService.class);
        service.executeFactoryForType(root);
        assertNotNull(session);
        assertNotNull(session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/test1")));
        assertNotNull(session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/test1/testFile.txt")));
        assertNotNull(session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/test1/testFile2.txt")));
        assertNotNull(session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/test1/testFile3.txt")));
        assertNotNull(session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/test1/nestedFolder/")));
        assertNotNull(session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/test1/nestedFolder/nestedFile.txt")));
    }

    @Test
    public void testData2ImportFactory() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.content.template.tests",
                "OSGI-INF/test-import-data2-content-template-contrib.xml");
        openSession();
        DocumentModel root = session.getDocument(new PathRef("/"));
        assertNotNull(root);
        service = Framework.getLocalService(ContentTemplateService.class);
        service.executeFactoryForType(root);
        assertNotNull(session);
        DocumentModel testZipfolder = session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/testZipImport"));
        assertNotNull(testZipfolder);
        DocumentModelList childs = session.getChildren(testZipfolder.getRef());
        assertNotNull(childs);
        assertEquals(childs.size(), 3);
        DocumentModel subNote = session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/testZipImport/SubFolder/SubNote"));
        assertNotNull(subNote);
        assertEquals(subNote.getType(), "Note");
    }

    @Test
    public void testData3ImportFactory() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.content.template.tests",
                "OSGI-INF/test-import-data3-content-template-contrib.xml");
        openSession();
        DocumentModel root = session.getDocument(new PathRef("/"));
        assertNotNull(root);
        service = Framework.getLocalService(ContentTemplateService.class);
        service.executeFactoryForType(root);

        DocumentModel helloDoc = session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/hello.pdf"));
        assertNotNull(helloDoc);
        assertEquals(helloDoc.getType(), "File");
    }

}
