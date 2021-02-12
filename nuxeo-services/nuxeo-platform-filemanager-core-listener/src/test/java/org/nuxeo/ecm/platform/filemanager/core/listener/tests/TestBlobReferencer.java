package org.nuxeo.ecm.platform.filemanager.core.listener.tests;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.filemanager.core.listener.BlobReferencer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.filemanager.api")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.core.listener:OSGI-INF/filemanager-digestcomputer-event-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.filemanager.core.listener:OSGI-INF/filemanager-blobreferencer-event-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.filemanager.core.listener.test:OSGI-INF/nxfilemanager-digest-contrib.xml")
public class TestBlobReferencer {

    // Blob 1
    private static String BLOB_1_CONTENT = "SOMEDUMMYDATA";

    private static String BLOB_1_NAME = "test.pdf";

    private static String DIGEST_1 = "CJz5xUykO51gRRCIQadZ9dL20NPDd/O0yVBEgP13Skg=";

    // Blob 2
    private static String BLOB_2_CONTENT = "MODIFIEDDUMMYDATA";

    private static String BLOB_2_NAME = "Mtest.pdf";

    private static String DIGEST_2 = "8Bz3MmKF3RbrlMMxXQjSsYE3AW3ZdGC6DMzA6EAt0Fw=";

    @Inject
    protected CoreSession coreSession;

    protected DocumentModel createFileDocument() {
        DocumentModel fileDoc = coreSession.createDocumentModel("/", "testFile", "File");
        fileDoc.setProperty("dublincore", "title", "TestFile");
        Blob blob = Blobs.createBlob(BLOB_1_CONTENT);
        blob.setFilename(BLOB_1_NAME);
        blob.setMimeType("application/pdf");
        fileDoc.setProperty("file", "content", blob);
        fileDoc = coreSession.createDocument(fileDoc);
        coreSession.save();
        return fileDoc;
    }

    protected DocumentModel changeFileBlob(DocumentRef docRef) {
        DocumentModel docModel = coreSession.getDocument(docRef);
        Blob blob = Blobs.createBlob(BLOB_2_CONTENT);
        blob.setFilename(BLOB_2_NAME);
        blob.setMimeType("application/pdf");
        docModel.setProperty("file", "content", blob);
        coreSession.saveDocument(docModel);
        coreSession.save();
        return docModel;
    }

    protected DocumentModel createOtherFileDocumentWithSameBlob() {
        DocumentModel fileDoc2 = coreSession.createDocumentModel("/", "testFile2", "File");
        fileDoc2.setProperty("dublincore", "title", "TestFile2");
        Blob blob = Blobs.createBlob(BLOB_1_CONTENT);
        blob.setFilename(BLOB_1_NAME);
        blob.setMimeType("application/pdf");
        fileDoc2.setProperty("file", "content", blob);
        fileDoc2 = coreSession.createDocument(fileDoc2);
        coreSession.save();
        return fileDoc2;
    }

    protected KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(BlobReferencer.BINARY_REFERENCES);
    }

    @Test
    public void testBlobReferencer() {
        // Create a doc with a blob
        DocumentModel file = createFileDocument();
        Blob blob = (Blob) file.getProperty("file", "content");
        assertEquals(DIGEST_1, blob.getDigest());
        KeyValueStore kv = getKvStore();

        assertEquals((Long) 1L, kv.getLong(DIGEST_1));

        // Create a second document with the same blob and check they have the same digest
        DocumentModel file2 = createOtherFileDocumentWithSameBlob();
        blob = (Blob) file2.getProperty("file", "content");
        assertEquals(DIGEST_1 ,blob.getDigest());

        assertEquals((Long) 2L, kv.getLong(DIGEST_1));

        // Modify the blob property on the first doc
        file = changeFileBlob(file.getRef());
        blob = (Blob) file.getProperty("file", "content");
        assertEquals(DIGEST_2 ,blob.getDigest());

        assertEquals((Long) 1L, kv.getLong(DIGEST_1));
        assertEquals((Long) 1L, kv.getLong(DIGEST_2));

        // Delete the second document
        coreSession.removeDocument(file2.getRef());
        assertEquals((Long) 0L, kv.getLong(DIGEST_1));
        assertEquals((Long) 1L, kv.getLong(DIGEST_2));

    }
}
