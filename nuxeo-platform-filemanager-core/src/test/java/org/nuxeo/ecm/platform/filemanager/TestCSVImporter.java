package org.nuxeo.ecm.platform.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.extension.CSVZipImporter;
import org.nuxeo.runtime.api.Framework;

public class TestCSVImporter extends RepositoryOSGITestCase {

    protected DocumentModel destWS;
    protected DocumentModel wsRoot;

    private File getArchiveFile() {
        return new File(FileUtils.getResourcePathFromContext("test-data/testCSVArchive.zip"));
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
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
        destWS = ws;
    }

    public void testArchiveDetection() throws IOException {
        ZipFile archive = CSVZipImporter.getArchiveFileIfValid(getArchiveFile());
        assertNotNull(archive);
        archive.close();
    }

    public void testImportViaFileManager() throws Exception {
        File archive = getArchiveFile();
        FileManager fm = Framework.getService(FileManager.class);
        Blob blob = new FileBlob(archive);
        fm.createDocumentFromBlob(coreSession, blob, destWS.getPathAsString(), true, "toto");
        DocumentModelList children = coreSession.getChildren(destWS.getRef());
        assertTrue(children.size() == 2);

        DocumentModel MyFile =  coreSession.getChild(destWS.getRef(), "myfile");
        DocumentModel MyNote =  coreSession.getChild(destWS.getRef(), "mynote");

        assertEquals("My File", MyFile.getTitle());
        assertEquals("My Note", MyNote.getTitle());

        assertEquals("this is text", MyNote.getProperty("note", "note"));
        assertNotNull(MyFile.getProperty("file", "content"));

    }

}
