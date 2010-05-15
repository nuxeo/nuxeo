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

import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.dam.core.Constants;
import org.nuxeo.dam.platform.context.ImportActions;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.richfaces.event.UploadEvent;
import org.junit.Test;

import com.google.inject.Inject;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
@Deploy({
        "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.mimetype.core",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.video.core",
        "org.nuxeo.ecm.platform.audio.core",
        "org.nuxeo.ecm.platform.filemanager.core:OSGI-INF/nxfilemanager-service.xml",
        "org.nuxeo.ecm.platform.filemanager.core:OSGI-INF/nxfilemanager-plugins-contrib.xml",
        "org.nuxeo.ecm.platform.content.template",
        "org.nuxeo.dam.core:OSGI-INF/dam-schemas-contrib.xml",
        "org.nuxeo.ecm.webapp.core:OSGI-INF/dam-filemanager-plugins-contrib.xml"
})
@LocalDeploy({
        "org.nuxeo.ecm.platform.audio.core:OSGI-INF/test-dam-content-template.xml"
})
public class TestZipImporter {

    @Inject
    protected CoreSession session;

    @Inject
    protected FileManager service;

    protected File getTestFile(String relativePath) {
        return new File(FileUtils.getResourcePathFromContext(relativePath));
    }

    @Test
    public void testImportZip() throws Exception {
        File file = getTestFile("test-data/test.zip");

        Blob input = StreamingBlob.createFromFile(file, "application/zip");

        DocumentModel doc = service.createDocumentFromBlob(session, input,
                session.getRootDocument().getPathAsString(), true, "test-data/test.zip");

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

        DocumentModel importSetRoot = session.getDocument(new PathRef(ImportActions.IMPORT_ROOT_PATH));
        importActions.setImportFolder(importSetRoot.getId());

        File file = getTestFile("test-data/test.zip");
        UploadEvent event = UploadItemMock.getUploadEvent(file);
        importActions.uploadListener(event);
        importSet.setProperty("dublincore", "title", "myimportset");

        importActions.createImportSet();

        importSet = session.getDocument(importSet.getRef());
        assertNotNull(importSet);

        String title = (String) importSet.getProperty("dublincore", "title");
        String type = importSet.getType();
        assertEquals("myimportset", title);
        assertEquals(Constants.IMPORT_SET_TYPE, type);

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
