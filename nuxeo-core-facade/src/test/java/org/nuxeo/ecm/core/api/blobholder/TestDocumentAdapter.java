package org.nuxeo.ecm.core.api.blobholder;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentStringBlobHolder;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;

public class TestDocumentAdapter extends RepositoryOSGITestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        openRepository();
    }

    public void testFileAdapters() throws Exception {

        DocumentModel file = getCoreSession().createDocumentModel("File");
        file.setPathInfo("/", "TestFile");

        Blob blob = new StringBlob("BlobContent");
        blob.setFilename("TestFile.txt");
        blob.setMimeType("text/plain");
        file.setProperty("dublincore", "title", "TestFile");
        file.setProperty("file", "content", blob);

        file = getCoreSession().createDocument(file);
        getCoreSession().save();

        BlobHolder bh = file.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        assertTrue(bh instanceof DocumentBlobHolder);

        assertEquals("/TestFile/TestFile.txt", bh.getFilePath());


    }
    public void testNoteAdapters() throws Exception {

        DocumentModel note = getCoreSession().createDocumentModel("Note");
        note.setPathInfo("/", "TestNote");

        note.setProperty("dublincore", "title", "TestNote");
        note.setProperty("note", "note", "Text of the note");

        note = getCoreSession().createDocument(note);

        BlobHolder bh = note.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        assertTrue(bh instanceof DocumentStringBlobHolder);

    }

    public void testFolderAdapters() throws Exception {
        DocumentModel folder = getCoreSession().createDocumentModel("Folder");
        folder.setPathInfo("/", "TestFolder");

        folder.setProperty("dublincore", "title", "TestFolder");

        folder = getCoreSession().createDocument(folder);


        BlobHolder bh = folder.getAdapter(BlobHolder.class);
        assertNull(bh);

    }







}
