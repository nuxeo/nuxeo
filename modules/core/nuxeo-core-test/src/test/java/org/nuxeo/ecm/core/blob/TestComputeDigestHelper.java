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
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.BLOB_DIGEST_UPDATED_NEW_DIGEST;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.BLOB_DIGEST_UPDATED_NEW_KEY;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.BLOB_DIGEST_UPDATED_OLD_DIGEST;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.BLOB_DIGEST_UPDATED_OLD_KEY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BLOB_DIGEST_UPDATED;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test:OSGI-INF/test-storage-blobstore-contrib.xml")
public class TestComputeDigestHelper {

    protected static final String FOO = "foo";

    protected static final String BAR = "bar";

    protected static final String FOO_MD5 = "acbd18db4cc2f85cedef654fccc4a4d8";

    protected static final String BAR_MD5 = "37b51d194a7513e45b56f6524f2d51f2";

    protected static final String FOO_SHA256 = "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Test
    public void testComputeDigest() throws IOException {
        InMemoryBlobProvider bp = new InMemoryBlobProvider();
        bp.initialize("mybp", Collections.singletonMap("digest", "SHA-256"));
        String key1 = bp.writeBlob(new BlobContext(new StringBlob(FOO), "mydocid", null));
        assertEquals(FOO_SHA256, key1); // sanity check, but that's not what this test is about

        // compute the digest using the async digest helper
        String digest = new ComputeDigestHelper("test", key1).computeDigest(bp.store);
        assertEquals(FOO_SHA256, digest);
        // missing blob
        digest = new ComputeDigestHelper("test", "nosuchkey").computeDigest(bp.store);
        assertNull(digest);
    }

    @Test
    public void testReplaceDigest() {
        assumeTrue("Blob digest replacement only on DBS", coreFeature.getStorageConfiguration().isDBS());

        DocumentModel doc = session.createDocumentModel("/", "doc1", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob(FOO));
        doc = session.createDocument(doc);
        session.save();

        ComputeDigestHelper helper = new ComputeDigestHelper("test", FOO_MD5);
        helper.newKey = BAR_MD5;
        helper.digest = BAR_MD5;

        helper.replaceDigestAllRepositories();
        session.save();
        doc.refresh();

        // check the blob is new
        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        assertEquals(BAR_MD5, blob.getKey());
        assertEquals(BAR_MD5, blob.getDigest());
    }

    @Test
    public void testCoreSessionReplaceBlobDigest() {
        assumeTrue("Blob digest replacement only on DBS", coreFeature.getStorageConfiguration().isDBS());

        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        doc1.setPropertyValue("file:content", (Serializable) Blobs.createBlob(FOO));
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = session.createDocumentModel("/", "doc", "File");
        doc2.setPropertyValue("file:content", (Serializable) Blobs.createBlob(BAR));
        doc2 = session.createDocument(doc2);
        session.save();

        try (CapturingEventListener listener = new CapturingEventListener(BLOB_DIGEST_UPDATED)) {

            // make doc1 point to doc2's blob
            String oldDigest = session.replaceBlobDigest(doc1.getRef(), FOO_MD5, BAR_MD5, BAR_MD5);
            assertEquals(FOO_MD5, oldDigest);

            // check event
            List<Event> events = listener.getCapturedEvents();
            assertEquals(1, events.size());
            Map<String, Serializable> properties = events.get(0).getContext().getProperties();
            assertEquals(FOO_MD5, properties.get(BLOB_DIGEST_UPDATED_OLD_KEY));
            assertEquals(FOO_MD5, properties.get(BLOB_DIGEST_UPDATED_OLD_DIGEST));
            assertEquals(BAR_MD5, properties.get(BLOB_DIGEST_UPDATED_NEW_KEY));
            assertEquals(BAR_MD5, properties.get(BLOB_DIGEST_UPDATED_NEW_DIGEST));
        }
        session.save();
        doc1.refresh();

        // check the blob is new
        ManagedBlob blob = (ManagedBlob) doc1.getPropertyValue("file:content");
        assertEquals(BAR_MD5, blob.getKey());
        assertEquals(BAR_MD5, blob.getDigest());
    }

}
