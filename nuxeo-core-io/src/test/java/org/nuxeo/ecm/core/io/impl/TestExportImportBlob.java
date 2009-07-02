package org.nuxeo.ecm.core.io.impl;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

import java.util.Map;

public class TestExportImportBlob extends SQLRepositoryTestCase {

    DocumentModel rootDocument;

    DocumentModel workspace;

    DocumentModel docToExport;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.api");

        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void createDocs() throws Exception {
        rootDocument = session.getRootDocument();
        workspace = session.createDocumentModel(rootDocument.getPathAsString(),
                "ws1", "Workspace");
        workspace.setProperty("dublincore", "title", "test WS");
        workspace = session.createDocument(workspace);

        docToExport = session.createDocumentModel(workspace.getPathAsString(),
                "file", "File");
        docToExport.setProperty("dublincore", "title", "MyDoc");

        Blob blob = new StringBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        blob.setMimeType("text/plain");
        docToExport.setProperty("file", "content", blob);

        docToExport = session.createDocument(docToExport);

        session.save();
    }

    public void testBlobFilenamePresent() throws Exception {
        createDocs();

        ExportedDocument exportedDoc = new ExportedDocumentImpl(docToExport, true);
        assertEquals("File", exportedDoc.getType());
//        assertEquals(1, exportedDoc.getBlobs().size());

//        Blob blob = getFirstBlob(exportedDoc.getBlobs());
//        assertEquals("dummyBlob.txt", blob.getFilename());

        session.removeDocument(docToExport.getRef());
        session.save();
        assertEquals(0, session.getChildren(workspace.getRef()).size());

        DocumentWriter writer = new DocumentModelWriter(session,
                rootDocument.getPathAsString());
        writer.write(exportedDoc);

        DocumentModelList children = session.getChildren(workspace.getRef());
        assertEquals(1, children.size());
        DocumentModel importedDocument = children.get(0);
        Blob blob = (Blob) importedDocument.getProperty("file", "content");
        assertEquals("dummyBlob.txt", blob.getFilename());
    }

    private Blob getFirstBlob(Map<String, Blob> blobs) {
        Blob blob = null;
        for (Blob b : blobs.values()) {
            blob = b;
            break;
        }
        return blob;
    }

}
