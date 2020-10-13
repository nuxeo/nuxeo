/*
 * (C) Copyright 2006 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 *
 *
 */

package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.ExportedZipImporter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Check IO archive import via Unit Tests.
 *
 * @author tiry
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.filemanager")
public class TestExportedZipImporterPlugin {

    protected DocumentModel sourceWS;

    protected DocumentModel destWS;

    protected DocumentModel wsRoot;

    @Inject
    protected CoreSession coreSession;

    private File archiveFile;

    public void createTestDocumentsAndArchive() throws Exception {
        wsRoot = coreSession.getDocument(new PathRef("/default-domain/workspaces"));

        DocumentModel ws = coreSession.createDocumentModel(wsRoot.getPathAsString(), "sourceWS", "Workspace");
        ws.setProperty("dublincore", "title", "Source Workspace");
        ws = coreSession.createDocument(ws);

        DocumentModel ws2 = coreSession.createDocumentModel(wsRoot.getPathAsString(), "destWS", "Workspace");
        ws2.setProperty("dublincore", "title", "Destination Workspace");
        ws2 = coreSession.createDocument(ws2);

        DocumentModel file = coreSession.createDocumentModel(ws.getPathAsString(), "myfile", "File");
        file.setProperty("dublincore", "title", "MyFile");
        file.setProperty("dublincore", "coverage", "MyFileCoverage");
        file = coreSession.createDocument(file);

        DocumentModel folder = coreSession.createDocumentModel(ws.getPathAsString(), "myfolder", "Folder");
        folder.setProperty("dublincore", "title", "MyFolder");
        folder = coreSession.createDocument(folder);

        DocumentModel subfile = coreSession.createDocumentModel(folder.getPathAsString(), "mysubfile", "File");
        subfile.setProperty("dublincore", "title", "MySubFile");
        subfile = coreSession.createDocument(subfile);

        DocumentReader reader = new DocumentTreeReader(coreSession, ws, false);

        archiveFile = File.createTempFile("TestExportedZipImporterPlugin_", ",zip");
        archiveFile.delete();
        DocumentWriter writer = new NuxeoArchiveWriter(archiveFile);

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        writer.close();
        reader.close();

        sourceWS = ws;
        destWS = ws2;
    }

    @After
    public void cleanupTempFolder() {
        FileUtils.deleteQuietly(archiveFile);
    }

    @Test
    public void testArchiveDetection() throws Exception {
        createTestDocumentsAndArchive();
        ZipFile archive = ExportedZipImporter.getArchiveFileIfValid(archiveFile);
        assertNotNull(archive);
        archive.close();
    }

    @Ignore(value = "NXP-26831")
    @Test
    public void testImportViaFileManager() throws Exception {
        createTestDocumentsAndArchive();
        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = Blobs.createBlob(archiveFile);
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, destWS.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        fm.createOrUpdateDocument(context);
        DocumentModelList children = coreSession.getChildren(destWS.getRef());
        assertTrue(children.size() > 0);
        DocumentModel importedWS = children.get(0);
        assertEquals(importedWS.getTitle(), sourceWS.getTitle());
        DocumentModelList subChildren = coreSession.getChildren(importedWS.getRef());
        assertSame(2, subChildren.size());

        DocumentModel subFolder = coreSession.getChild(importedWS.getRef(), "myfolder");
        assertNotNull(subFolder);

        DocumentModel subFile = coreSession.getChild(importedWS.getRef(), "myfile");
        assertNotNull(subFile);

        DocumentModelList subSubChildren = coreSession.getChildren(subFolder.getRef());
        assertSame(1, subSubChildren.size());
    }

    @Ignore(value = "NXP-26831")
    @Test
    public void testOverrideImportViaFileManager() throws Exception {
        createTestDocumentsAndArchive();
        // first update the source DM of the exported source
        sourceWS.setProperty("dublincore", "title", "I have been changed");
        sourceWS = coreSession.saveDocument(sourceWS);

        // remove one children
        DocumentModel subFile = coreSession.getChild(sourceWS.getRef(), "myfile");
        coreSession.removeDocument(subFile.getRef());
        coreSession.save();

        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = Blobs.createBlob(archiveFile);
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, wsRoot.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        fm.createOrUpdateDocument(context);
        sourceWS = coreSession.getChild(wsRoot.getRef(), "sourceWS");
        assertNotNull(sourceWS);
        assertEquals("Source Workspace", sourceWS.getTitle());

        subFile = coreSession.getChild(sourceWS.getRef(), "myfile");
        assertNotNull(subFile);
    }

}
