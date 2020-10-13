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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.zip.ZipFile;

import javax.inject.Inject;

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

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.filemanager")
public class TestCSVImporter {

    @Inject
    protected CoreSession coreSession;

    protected DocumentModel destWS;

    protected DocumentModel wsRoot;

    private static File getArchiveFile() {
        return new File(FileUtils.getResourcePathFromContext("test-data/testCSVArchive.zip"));
    }

    public void createTestDocuments() throws Exception {
        wsRoot = coreSession.getDocument(new PathRef("/default-domain/workspaces"));

        DocumentModel ws = coreSession.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = coreSession.createDocument(ws);
        destWS = ws;
    }

    @Test
    public void testArchiveDetection() throws Exception {
        createTestDocuments();
        ZipFile archive = CSVZipImporter.getArchiveFileIfValid(getArchiveFile());
        assertNotNull(archive);
        archive.close();
    }

    @Test
    public void testImportViaFileManager() throws Exception {
        createTestDocuments();
        File archive = getArchiveFile();
        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = Blobs.createBlob(archive);
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, destWS.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        fm.createOrUpdateDocument(context);
        DocumentModelList children = coreSession.getChildren(destWS.getRef());
        assertSame(2, children.size());

        DocumentModel MyFile = coreSession.getChild(destWS.getRef(), "myfile");
        DocumentModel MyNote = coreSession.getChild(destWS.getRef(), "mynote");

        assertEquals("My File", MyFile.getTitle());
        assertEquals("My Note", MyNote.getTitle());

        assertEquals("this is text", MyNote.getProperty("note", "note"));
        assertNotNull(MyFile.getProperty("file", "content"));
    }

}
