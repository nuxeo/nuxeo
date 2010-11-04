/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 *
 */

package org.nuxeo.ecm.platform.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.ExportedZipImporter;
import org.nuxeo.runtime.api.Framework;

/**
 * Check IO archive import via Unit Tests.
 *
 * @author tiry
 */
public class TestExportedZipImporterPlugin extends RepositoryOSGITestCase {

    private String archiveFileName;

    protected final String tmpDir = System.getProperty("java.io.tmpdir");

    protected DocumentModel sourceWS;

    protected DocumentModel destWS;

    protected DocumentModel wsRoot;

    private File getArchiveFile() {
        return new File(archiveFileName);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        openRepository();

        createTestDocuments();
    }

    private void createTestDocuments() throws Exception {
        wsRoot = coreSession.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = coreSession.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = coreSession.createDocument(ws);

        DocumentModel ws2 = coreSession.createDocumentModel(
                wsRoot.getPathAsString(), "ws2", "Workspace");
        ws2.setProperty("dublincore", "title", "test WS2");
        ws2 = coreSession.createDocument(ws2);

        DocumentModel file = coreSession.createDocumentModel(
                ws.getPathAsString(), "myfile", "File");
        file.setProperty("dublincore", "title", "MyFile");
        file.setProperty("dublincore", "coverage", "MyFileCoverage");
        file = coreSession.createDocument(file);

        DocumentModel folder = coreSession.createDocumentModel(
                ws.getPathAsString(), "myfolder", "Folder");
        folder.setProperty("dublincore", "title", "MyFolder");
        folder = coreSession.createDocument(folder);

        DocumentModel subfile = coreSession.createDocumentModel(
                folder.getPathAsString(), "mysubfile", "File");
        subfile.setProperty("dublincore", "title", "MySubFile");
        subfile = coreSession.createDocument(subfile);

        DocumentReader reader = new DocumentTreeReader(coreSession, ws, false);

        archiveFileName = tmpDir + System.getProperty("file.separator")
                + "Testing" + System.currentTimeMillis();
        File archiveFile = new File(archiveFileName);

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

    public void testArchiveDetection() throws IOException {
        ZipFile archive = ExportedZipImporter.getArchiveFileIfValid(getArchiveFile());
        assertNotNull(archive);
        archive.close();
    }

    public void testImportViaFileManager() throws Exception {
        File archive = getArchiveFile();
        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = new FileBlob(archive);
        fm.createDocumentFromBlob(coreSession, blob, destWS.getPathAsString(),
                true, "toto.zip");
        DocumentModelList children = coreSession.getChildren(destWS.getRef());
        assertTrue(children.size() > 0);
        assertEquals(children.get(0).getTitle(), sourceWS.getTitle());

        DocumentModel importedWS = children.get(0);
        DocumentModelList subChildren = coreSession.getChildren(importedWS.getRef());
        assertSame(2, subChildren.size());

        DocumentModel subFolder = coreSession.getChild(importedWS.getRef(),
                "myfolder");
        assertNotNull(subFolder);

        DocumentModel subFile = coreSession.getChild(importedWS.getRef(),
                "myfile");
        assertNotNull(subFile);

        DocumentModelList subSubChildren = coreSession.getChildren(subFolder.getRef());
        assertSame(1, subSubChildren.size());
    }

    public void testOverrideImportViaFileManager() throws Exception {
        // first update the source DM of the exported source
        sourceWS.setProperty("dublincore", "title", "I have been changed");
        sourceWS = coreSession.saveDocument(sourceWS);

        // remove one children
        DocumentModel subFile = coreSession.getChild(sourceWS.getRef(),
                "myfile");
        coreSession.removeDocument(subFile.getRef());
        coreSession.save();

        File archive = getArchiveFile();
        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = new FileBlob(archive);
        fm.createDocumentFromBlob(coreSession, blob, wsRoot.getPathAsString(),
                true, "toto.zip");
        sourceWS = coreSession.getChild(wsRoot.getRef(), "ws1");
        assertNotNull(sourceWS);
        assertEquals("test WS", sourceWS.getTitle());

        subFile = coreSession.getChild(sourceWS.getRef(), "myfile");
        assertNotNull(subFile);
    }

}
