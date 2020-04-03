/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.test.FulltextStoredInBlobFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(FulltextStoredInBlobFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-blob-dispatcher-fulltext.xml")
public class TestFulltextStoredInBlobNoQuery extends TestFulltextAbstractNoQuery {

    // from XML config
    protected static final String FULLTEXT_BLOB_PROVIDER = "fulltext";

    @Inject
    protected BlobManager blobManager;

    @Inject
    protected DocumentBlobManager documentBlobManager;

    @Override
    protected boolean expectBinaryText() {
        // binary text available when stored in blob
        return true;
    }

    @Override
    @Test
    public void testBinaryText() throws IOException {
        super.testBinaryText();

        // check that there is a blob with the fulltext
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = FULLTEXT_BLOB_PROVIDER + ":" + BINARY_TEXT_MD5;
        Blob blob = documentBlobManager.readBlob(blobInfo, session.getRepositoryName());
        assertEquals(BINARY_TEXT, blob.getString());

        // check that the blob is in its own blob provider
        BlobProvider ftbp = blobManager.getBlobProvider(FULLTEXT_BLOB_PROVIDER);
        BlobInfo bi = new BlobInfo();
        bi.key = BINARY_TEXT_MD5;
        assertEquals(BINARY_TEXT, ftbp.readBlob(bi).getString());

        // check that we can GC and the fulltext blob is still here
        sleepBeforeGC();
        BinaryManagerStatus status = documentBlobManager.garbageCollectBinaries(true);
        assertEquals(2, status.numBinaries); // main blob + fulltext blob
        assertEquals(0, status.numBinariesGC);
        blob = documentBlobManager.readBlob(blobInfo, session.getRepositoryName());
        assertEquals(BINARY_TEXT, blob.getString());

        // remove doc
        session.removeDocument(new PathRef("/doc"));
        session.save();
        coreFeature.waitForAsyncCompletion();

        // after GC the fulltext blob is gone
        sleepBeforeGC();
        status = documentBlobManager.garbageCollectBinaries(true);
        assertEquals(0, status.numBinaries);
        assertEquals(2, status.numBinariesGC);
        try {
            blob = documentBlobManager.readBlob(blobInfo, session.getRepositoryName());
            // for BlobStore-derived implementations an empty stream is returned
            assertNotNull(blob);
            assertEquals("", blob.getString());
        } catch (IOException e) {
            // for other implementations we get an exception
            assertEquals("Unknown binary: " + BINARY_TEXT_MD5, e.getMessage());
        }
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

}
