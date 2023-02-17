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
package org.nuxeo.ecm.blob.s3;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.awaitility.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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
@Features({ CoreFeature.class, S3BlobProviderFeature.class })
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/disable-schedulers.xml")
public class TestS3DocumentBlobGC {

    protected static final Duration AWAIT_DURATION = Duration.TWO_SECONDS;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentBlobManager documentBlobManager;

    @Test
    public void testDocumentBlobDelete() throws IOException {
        assumeTrue("MongoDB feature only", !coreFeature.getStorageConfiguration().isVCS());
        final String CONTENT = "hello world";
        // Create 2 docs referencing the same main blob as main content
        // and misc attachements
        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        doc1.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT));
        doc1.setPropertyValue("files:files", (Serializable) List.of(Map.of("file", Blobs.createBlob("attachement1")),
                Map.of("file", Blobs.createBlob("attachement2"))));
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "File");
        doc2.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT));
        doc2.setPropertyValue("files:files", (Serializable) List.of(Map.of("file", Blobs.createBlob("attachement2")),
                Map.of("file", Blobs.createBlob("attachement3"))));
        doc2 = session.createDocument(doc2);
        session.save();

        ManagedBlob blob1 = (ManagedBlob) doc1.getPropertyValue("file:content");
        ManagedBlob blob2 = (ManagedBlob) doc2.getPropertyValue("file:content");
        ManagedBlob attachement11 = (ManagedBlob) doc1.getPropertyValue("files:files/0/file");
        ManagedBlob attachement12 = (ManagedBlob) doc1.getPropertyValue("files:files/1/file");
        ManagedBlob attachement21 = (ManagedBlob) doc2.getPropertyValue("files:files/0/file");
        ManagedBlob attachement22 = (ManagedBlob) doc2.getPropertyValue("files:files/1/file");
        assertEquals(blob1.getKey(), blob2.getKey());
        assertEquals(attachement12.getKey(), attachement21.getKey());

        session.removeDocument(doc1.getRef());
        coreFeature.waitForAsyncCompletion();
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob1.getProviderId());
        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNull(blobProvider.getFile(attachement11));
            assertNotNull(blobProvider.getFile(blob1));
            assertNotNull(blobProvider.getFile(blob2));
            // attachement12 is not deleted because it is the same than
            // attachement21 that is still referenced by doc2
            assertNotNull(blobProvider.getFile(attachement12));
            assertNotNull(blobProvider.getFile(attachement21));
            assertNotNull(blobProvider.getFile(attachement22));
        });

        session.removeDocument(doc2.getRef());
        coreFeature.waitForAsyncCompletion();

        // Assert blobs does not exist anymore
        await().atMost(AWAIT_DURATION).untilAsserted(() -> {
            assertNull(blobProvider.getFile(blob1));
            assertNull(blobProvider.getFile(blob2));
            assertNull(blobProvider.getFile(attachement11));
            assertNull(blobProvider.getFile(attachement12));
            assertNull(blobProvider.getFile(attachement21));
            assertNull(blobProvider.getFile(attachement22));
        });
    }

}
