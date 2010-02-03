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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.fileimporter;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.dam.platform.context.ImportActions;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryJUnit4;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.UploadEvent;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestZipImporter extends SQLRepositoryJUnit4 {

    protected FileManager service;

    protected DocumentModel root;

    public TestZipImporter() {
        super("TestZipImporter");
    }

    @Before
    public void setUp() throws Exception {
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.video.core");
        deployBundle("org.nuxeo.ecm.platform.audio.core");
        deployBundle("org.nuxeo.ecm.platform.content.template");

        deployContrib("org.nuxeo.ecm.platform.filemanager.core",
                "OSGI-INF/nxfilemanager-service.xml");
        deployContrib("org.nuxeo.ecm.platform.filemanager.core",
                "OSGI-INF/nxfilemanager-plugins-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.picture.web",
                "OSGI-INF/picturebook-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.picture.web",
                "OSGI-INF/imaging-various-contrib.xml");
        deployContrib("org.nuxeo.ecm.webapp.core",
                "OSGI-INF/dam-filemanager-plugins-contrib.xml");
        deployContrib("org.nuxeo.dam.core",
                "OSGI-INF/dam-schemas-contrib.xml");
        deployContrib("org.nuxeo.ecm.webapp.tests",
                "OSGI-INF/test-dam-content-template.xml");

        openSession();
        service = Framework.getService(FileManager.class);
        root = session.getRootDocument();
    }

    @After
    public void tearDown() throws Exception {
        service = null;
        root = null;

        undeployContrib("org.nuxeo.ecm.webapp.core",
                "OSGI-INF/dam-filemanager-plugins-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.picture.web",
                "OSGI-INF/imaging-various-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.picture.web",
                "OSGI-INF/picturebook-types-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.filemanager.core",
                "OSGI-INF/nxfilemanager-plugins-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.filemanager.core",
                "OSGI-INF/nxfilemanager-service.xml");
        undeployContrib("org.nuxeo.dam.core",
                "OSGI-INF/dam-schemas-contrib.xml");
        undeployContrib("org.nuxeo.ecm.webapp.tests",
                "OSGI-INF/test-dam-content-template.xml");
    }

    protected File getTestFile(String relativePath) {
        return new File(FileUtils.getResourcePathFromContext(relativePath));
    }

    @Test
    public void testImportZip() throws Exception {
        File file = getTestFile("test-data/test.zip");

        Blob input = StreamingBlob.createFromFile(file, "application/zip");

        DocumentModel doc = service.createDocumentFromBlob(session, input,
                root.getPathAsString(), true, "test-data/test.zip");

        DocumentModel child = session.getChild(doc.getRef(), "plain");
        assertNotNull(child);
        assertEquals("Note", child.getType());

        child = session.getChild(doc.getRef(), "image");
        assertNotNull(child);
        assertEquals("Picture", child.getType());

        child = session.getChild(doc.getRef(), "spreadsheet");
        assertNotNull(child);
        assertEquals("File", child.getType());

        // names are converted to lowercase
        child = session.getChild(doc.getRef(), "samplempg");
        assertNotNull(child);
        assertEquals("Video", child.getType());

        child = session.getChild(doc.getRef(), "samplewav");
        assertNotNull(child);
        assertEquals("Audio", child.getType());


    }

    @Test
    public void testImportSetCreation() throws Exception {

        ImportActions importActions = new ImportActionsMock(session,
                service);

        DocumentModel importSet = importActions.getNewImportSet();
        // test that we have a default title
        String defaultTitle = (String) importSet.getProperty("dublincore", "title");
        assertNotNull(defaultTitle);
        assertTrue(defaultTitle.startsWith("Administrator"));

        File file = getTestFile("test-data/test.zip");
        UploadEvent event = UploadItemMock.getUploadEvent(file);
        importActions.uploadListener(event);
        importSet.setProperty("dublincore", "title", "myimportset");

        importActions.createImportSet();

        importSet = session.getDocument(importSet.getRef());
        assertNotNull(importSet);
        String title = (String) importSet.getProperty("dublincore", "title");
        String type = importSet.getType();
        assertEquals(title, "myimportset");
        assertEquals(type, "ImportSet");

        DocumentModelList children = session.getChildren(importSet.getRef());
        assertNotNull(children);
        assertEquals(5, children.size());

        DocumentModel child = session.getChild(importSet.getRef(), "plain");
        assertNotNull(child);
        assertEquals("Note", child.getType());

        child = session.getChild(importSet.getRef(), "image");
        assertNotNull(child);
        assertEquals("Picture", child.getType());

        child = session.getChild(importSet.getRef(), "spreadsheet");
        assertNotNull(child);
        assertEquals("File", child.getType());

        // names are converted to lowercase
        child = session.getChild(importSet.getRef(), "samplewav");
        assertNotNull(child);
        assertEquals("Audio", child.getType());

        child = session.getChild(importSet.getRef(), "samplempg");
        assertNotNull(child);
        assertEquals("Video", child.getType());
    }

}
