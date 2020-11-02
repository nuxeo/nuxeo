/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *     Jackie Aldama <jaldama@nuxeo.com>
 */
package org.nuxeo.ecm.platform.filemanager;

import java.io.File;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.CSVZipImporter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.core.tests:faceted-tag-core-types-contrib.xml")
public class TestCSVZipImporter {

    @Inject
    protected CoreSession coreSession;

    protected DocumentModel workspace1;

    protected DocumentModel workspace2;

    protected DocumentModel wsRoot;

    @Before
    public void createTestDocuments() throws Exception {
        wsRoot = coreSession.getDocument(new PathRef("/default-domain/workspaces"));

        workspace1 = coreSession.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
        workspace1.setProperty("dublincore", "title", "test WS1");
        workspace1 = coreSession.createDocument(workspace1);

        workspace2 = coreSession.createDocumentModel(wsRoot.getPathAsString(), "ws2", "Workspace");
        workspace2.setProperty("dublincore", "title", "test WS2");
        workspace2 = coreSession.createDocument(workspace2);
    }

    @Test
    public void testArchiveDetection() throws Exception {
         ZipFile archive = CSVZipImporter.getArchiveFileIfValid(getArchiveFile("test-data/completeZipTestArchive.zip"));
        assertNotNull(archive);
        archive.close();
    }

    @Test
    public void testDocumentCreationFailureWithNoName() throws Exception {
        File archive = getArchiveFile("test-data/testNoNameCSVArchive.zip");
        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = Blobs.createBlob(archive);
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, workspace2.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        fm.createOrUpdateDocument(context);
        DocumentModelList children = coreSession.getChildren(workspace2.getRef());
        assertEquals(0, children.size());
    }

    @Test
    public void testImportViaFileManager() throws Exception {
        File archive = getArchiveFile("test-data/completeZipTestArchive.zip");
        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = Blobs.createBlob(archive);
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, workspace1.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        fm.createOrUpdateDocument(context);
        DocumentModelList children = coreSession.getChildren(workspace1.getRef());
        assertEquals(2, children.size());

        DocumentModel MyFile = coreSession.getChild(workspace1.getRef(), "My File");
        DocumentModel MyNote = coreSession.getChild(workspace1.getRef(), "MyNote");

        assertEquals("My File", MyFile.getTitle());
        assertEquals("My Note", MyNote.getTitle());

        assertEquals("My File", MyFile.getName());
        assertEquals("MyNote", MyNote.getName());

        // Validate some properties
        assertEquals("this is text", MyNote.getPropertyValue("note:note"));
        assertNotNull(MyFile.getPropertyValue("file:content"));

        // Validate MultiValue properties can be imported
        List<String> contributors = Arrays.asList((String[]) MyFile.getPropertyValue("dc:contributors"));
        assertEquals(4, contributors.size());

        // Validate lifecycle state can be imported
        assertEquals("project", MyFile.getCurrentLifeCycleState());
        assertEquals("approved", MyNote.getCurrentLifeCycleState());

        // Validate tags can be imported
        List<HashMap> tags = (List<HashMap>) MyFile.getPropertyValue("nxtag:tags");
        assertEquals(1, tags.size());
        assertEquals("mynewtag", tags.get(0).get("label"));
        assertEquals(2, ((List<String>) MyNote.getPropertyValue("nxtag:tags")).size());
    }

    protected static File getArchiveFile(String file) {
        return new File(FileUtils.getResourcePathFromContext(file));
    }
}