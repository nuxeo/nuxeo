/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.awaitility.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2023
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/disable-schedulers.xml")
public class TestDocumentBlobGC {

    protected static final Duration AWAIT_DURATION = Duration.TWO_SECONDS;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentBlobManager documentBlobManager;

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-delete.xml")
    public void testSharedBlobDelete() throws IOException {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());
        final String CONTENT = "hello world";
        // Create 2 docs referencing the same blob as main content
        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        doc1.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT));
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "File");
        doc2.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT));
        doc2 = session.createDocument(doc2);
        session.save();

        // check that the doc shares the 2 blobs
        ManagedBlob blob1 = (ManagedBlob) doc1.getPropertyValue("file:content");
        ManagedBlob blob2 = (ManagedBlob) doc2.getPropertyValue("file:content");
        assertEquals(blob1.getKey(), blob2.getKey());

        String key = blob1.getKey();

        assertFalse(documentBlobManager.deleteBlob(doc2.getRepositoryName(), key, false));

        session.removeDocument(doc1.getRef());
        assertFalse(documentBlobManager.deleteBlob(doc2.getRepositoryName(), key, false));

        session.removeDocument(doc2.getRef());
        assertTrue(documentBlobManager.deleteBlob(doc2.getRepositoryName(), key, false));

        // Assert blob does not exist anymore
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob1.getProviderId());
        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNull(blobProvider.getFile(blob1));
        });
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-multi-repo-delete.xml")
    public void testBlobDeleteMultiRepo() throws IOException {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());
        final String CONTENT = "multiRepo";
        // Create 2 docs in 2 different repos but referencing the same blob as main content
        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        doc1.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT));
        doc1 = session.createDocument(doc1);
        CoreSession session2 = CoreInstance.getCoreSession("test2");
        DocumentModel doc2 = session2.createDocumentModel("/", "doc2", "File");
        doc2.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT));
        doc2 = session2.createDocument(doc2);
        session2.save();

        // check that the doc shares the 2 blobs
        ManagedBlob blob1 = (ManagedBlob) doc1.getPropertyValue("file:content");
        ManagedBlob blob2 = (ManagedBlob) doc2.getPropertyValue("file:content");
        assertEquals(blob1.getKey(), blob2.getKey());

        String key = blob1.getKey();
        // keys are unprefixed in multi repos
        assertTrue(key.indexOf(':') < 0);

        BlobProvider blobProvider1 = Framework.getService(BlobManager.class).getBlobProvider(blob1.getProviderId());
        BlobProvider blobProvider2 = Framework.getService(BlobManager.class).getBlobProvider(blob2.getProviderId());
        assertNotNull(blobProvider1.getFile(blob1));
        assertNotNull(blobProvider2.getFile(blob2));

        // Remove 1st doc
        session.removeDocument(doc1.getRef());
        coreFeature.waitForAsyncCompletion();
        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNull(blobProvider1.getFile(blob1));
        });
        // 2nd blob has not been deleted
        assertNotNull(blobProvider2.getFile(blob2));

        // Remove 2nd doc
        session2.removeDocument(doc2.getRef());
        coreFeature.waitForAsyncCompletion();
        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNull(blobProvider2.getFile(blob2));
        });
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-delete.xml")
    public void testDryRun() throws IOException {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("dry run"));
        doc = session.createDocument(doc);
        session.save();

        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        String key = blob.getKey();

        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob.getProviderId());
        assertFalse(documentBlobManager.deleteBlob(doc.getRepositoryName(), key, true));
        assertNotNull(blobProvider.getFile(blob));

        session.removeDocument(doc.getRef());
        assertTrue(documentBlobManager.deleteBlob(doc.getRepositoryName(), key, false));
        coreFeature.waitForAsyncCompletion();

        // Assert blob does not exist anymore
        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNull(blobProvider.getFile(blob));
        });
    }

    @Test
    public void testUnsupportedDeleteBlobOnVCS() {
        assumeTrue(
                "This test is to make sure repos without ecm:blobKeys capabilities will not delete blobs that MUST not be deleted.",
                coreFeature.getStorageConfiguration().isVCS());
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("UnsupportedDeleteBlobOnVCS"));
        doc = session.createDocument(doc);
        session.save();

        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        String key = blob.getKey();
        String repoName = doc.getRepositoryName();

        assertThrows(UnsupportedOperationException.class, () -> documentBlobManager.deleteBlob(repoName, key, true));
        assertThrows(UnsupportedOperationException.class, () -> documentBlobManager.deleteBlob(repoName, key, false));

        coreFeature.waitForAsyncCompletion();
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob.getProviderId());
        assertNotNull(blobProvider.getFile(blob));

        session.removeDocument(doc.getRef());
        coreFeature.waitForAsyncCompletion();
        assertNotNull(blobProvider.getFile(blob));
    }

    @Test
    public void testIllegalDeleteBlobNullRepositoryName() {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("IllegalDeleteBlobNullRepositoryName"));
        doc = session.createDocument(doc);
        session.save();

        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        String key = blob.getKey();

        assertThrows(IllegalArgumentException.class, () -> documentBlobManager.deleteBlob(null, key, false));
        assertThrows(IllegalArgumentException.class, () -> documentBlobManager.deleteBlob("", key, false));

        coreFeature.waitForAsyncCompletion();
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob.getProviderId());
        assertNotNull(blobProvider.getFile(blob));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-delete.xml")
    public void testDeleteBlobAfterRemoveDocument() {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());
        DocumentModel doc = session.createDocumentModel("/", "doc1", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("toBeRemoved"));
        doc = session.createDocument(doc);
        session.save();
        DocumentRef ref = doc.getRef();
        ManagedBlob blob = (ManagedBlob) session.getDocument(ref).getPropertyValue("file:content");
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob.getProviderId());
        assertNotNull(blobProvider.getFile(blob));
        session.removeDocument(ref);
        coreFeature.waitForAsyncCompletion();

        assertFalse(session.exists(ref));

        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNull(blobProvider.getFile(blob));
        });
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-delete.xml")
    public void testDeleteBlobAfterRecursiveRemoveDocument() {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());
        // Create a couple of docs under a common root
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        session.save();
        List<ManagedBlob> blobs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("/folder", "doc" + i, "File");
            doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("toBeRemoved" + i));
            doc = session.createDocument(doc);
            session.save();
            blobs.add((ManagedBlob) session.getDocument(doc.getRef()).getPropertyValue("file:content"));
        }

        // Remove the root and check the recursive delete also deleted the descendant's blobs
        session.removeDocument(folder.getRef());
        coreFeature.waitForAsyncCompletion();
        blobs.forEach(blob -> {
            BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob.getProviderId());
            await().atMost(AWAIT_DURATION).untilAsserted(() -> {
                assertNull(blobProvider.getFile(blob));
            });
        });
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-delete.xml")
    public void testDeleteBlobAfterEditBlobProperty() {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());
        DocumentModel doc = session.createDocumentModel("/", "doc1", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("before"));
        doc = session.createDocument(doc);
        session.save();

        DocumentRef ref = doc.getRef();
        ManagedBlob blob = (ManagedBlob) session.getDocument(ref).getPropertyValue("file:content");
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob.getProviderId());
        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNotNull(blobProvider.getFile(blob));
        });

        // Replace blob
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("after"));
        doc = session.saveDocument(doc);
        coreFeature.waitForAsyncCompletion();

        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNull(blobProvider.getFile(blob));
        });
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-shared-storage-delete.xml")
    public void testUnsupportedDeleteBlobOnSharedStorage() {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("shared GC"));
        doc = session.createDocument(doc);
        session.save();

        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        String key = blob.getKey();

        String repoName = doc.getRepositoryName();
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob.getProviderId());
        assertThrows(UnsupportedOperationException.class, () -> documentBlobManager.deleteBlob(repoName, key, false));
        assertNotNull(blobProvider.getFile(blob));

        session.removeDocument(doc.getRef());
        coreFeature.waitForAsyncCompletion();
        assertNotNull(blobProvider.getFile(blob));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-dispatcher-delete.xml")
    public void testDeleteBlobAfterDispatch() {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());

        // Create 2 docs referencing the same blob as main content
        Blob b = Blobs.createBlob("dispatch");
        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        doc1.setPropertyValue("file:content", (Serializable) b);
        doc1 = session.createDocument(doc1);
        DocumentRef ref1 = doc1.getRef();
        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "File");
        doc2.setPropertyValue("file:content", (Serializable) b);
        doc2 = session.createDocument(doc1);
        session.save();
        coreFeature.waitForAsyncCompletion();

        // Remove 1st doc
        ManagedBlob blob = (ManagedBlob) session.getDocument(ref1).getPropertyValue("file:content");
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob.getProviderId());
        assertNotNull(blobProvider.getFile(blob));
        session.removeDocument(ref1);
        coreFeature.waitForAsyncCompletion();

        assertFalse(session.exists(ref1));

        // Assert blob still exists because it is still referenced
        assertNotNull(blobProvider.getFile(blob));

        // Force 2nd blob to move to another blob store (with blob dispatcher rule)
        doc2.setPropertyValue("dc:source", "foo");
        doc2 = session.saveDocument(doc2);
        coreFeature.waitForAsyncCompletion();

        // Assert blob does not exist anymore
        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNull(blobProvider.getFile(blob));
        });
    }

}
