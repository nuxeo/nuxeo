/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.blob.LocalBlobProvider;
import org.nuxeo.ecm.core.blob.LocalBlobStore.LocalBlobGarbageCollector;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/disable-schedulers.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-blob-gc-same-storage.xml")
public class TestBlobGCSharedStorage {

    protected static final String CONTENT1 = "hello world";

    protected static final String CONTENT2 = "bonjour le monde";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected BlobManager blobManager;

    @Inject
    protected DocumentBlobManager documentBlobManager;

    @Test
    public void testGCSharedStorage() throws IOException {
        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        doc1.setPropertyValue("dc:source", "first");
        doc1.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT1));
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "File");
        doc2.setPropertyValue("dc:source", "second");
        doc2.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT2));
        doc2 = session.createDocument(doc2);
        session.save();

        // check that the two blobs were dispatched to different blob providers
        ManagedBlob blob1 = (ManagedBlob) doc1.getPropertyValue("file:content");
        ManagedBlob blob2 = (ManagedBlob) doc2.getPropertyValue("file:content");
        assertTrue(blob1.getKey(), blob1.getKey().startsWith("first:"));
        assertTrue(blob2.getKey(), blob2.getKey().startsWith("second:"));

        // tweak GC to not use on-disk marking, to be representative of other GCs
        setMarkInMemory("first");
        setMarkInMemory("second");

        // check that when the GC runs nothing gets deleted
        sleepBeforeGC();
        BinaryManagerStatus status = documentBlobManager.garbageCollectBinaries(true);
        assertEquals(4, status.numBinaries); // two binaries seen twice
        assertEquals(0, status.numBinariesGC); // nothing deleted

        // check that the blobs are still here
        blob1 = (ManagedBlob) doc1.getPropertyValue("file:content");
        assertEquals(CONTENT1, blob1.getString());
        blob2 = (ManagedBlob) doc2.getPropertyValue("file:content");
        assertEquals(CONTENT2, blob2.getString());
    }

    // sleep before GC to pass its time threshold
    protected void sleepBeforeGC() {
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    protected void setMarkInMemory(String blobProviderId) {
        BlobProvider bp = blobManager.getBlobProvider(blobProviderId);
        BinaryGarbageCollector gc = ((LocalBlobProvider) bp).store.getBinaryGarbageCollector();
        ((LocalBlobGarbageCollector) gc).markInMemory = true;
    }

}
