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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.CSVZipImporter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", init = RepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core",
        "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.filemanager.core" })
public class TestCSVImporter {

    @Inject
    protected CoreSession coreSession;

    protected DocumentModel destWS;

    protected DocumentModel wsRoot;

    private static File getArchiveFile() {
        return new File(
                FileUtils.getResourcePathFromContext("test-data/testCSVArchive.zip"));
    }

    public void createTestDocuments() throws Exception {
        wsRoot = coreSession.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = coreSession.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
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
        Blob blob = new FileBlob(archive);
        fm.createDocumentFromBlob(coreSession, blob, destWS.getPathAsString(),
                true, "toto");
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
