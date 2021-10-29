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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.blob.s3.S3BlobProviderFeature.S3_DOC_TYPE;

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, S3BlobProviderFeature.class })
public class TestS3BlobProviderWithDocument {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    @Test
    public void testStoreToBlobProviderTest() throws IOException {
        testStoreToBlobProvider("test");
    }

    @Test
    public void testStoreToBlobProviderOther() throws IOException {
        testStoreToBlobProvider("other");
    }

    protected void testStoreToBlobProvider(String blobProviderId) throws IOException {
        String blobContent = "A simple blob";
        DocumentModel doc = session.createDocumentModel("/", "document", S3_DOC_TYPE);
        doc.setPropertyValue(String.format("s3-%s:content", blobProviderId),
                (Serializable) Blobs.createBlob(blobContent));
        doc = session.createDocument(doc);
        txFeature.nextTransaction(); // avoid an error in FulltextExtractorWork -> ManagedBlob

        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue(String.format("s3-%s:content", blobProviderId));
        String digest = blob.getDigest(); // digest is the key
        assertEquals(blobProviderId, blob.getProviderId());
        assertEquals(blobProviderId + ':' + digest, blob.getKey());
        assertEquals(blobContent, blob.getString());
    }

    // NXP-30632
    @Test
    public void testMoveFromBlobProviderTestToBlobProviderOther() throws IOException {
        testMoveFromTestToOther();
    }

    // NXP-30632
    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-feature-blob-provider-s3-other-managed.xml")
    public void testMoveFromBlobProviderTestToBlobProviderOtherWithManagedKey() throws IOException {
        testMoveFromTestToOther();
    }

    protected void testMoveFromTestToOther() throws IOException {
        String blobContent = "A simple blob";
        DocumentModel doc = session.createDocumentModel("/", "document", S3_DOC_TYPE);
        doc.setPropertyValue("s3-test:content", (Serializable) Blobs.createBlob(blobContent));
        doc = session.createDocument(doc);

        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("s3-test:content");
        doc.setPropertyValue("s3-test:content", null);
        doc.setPropertyValue("s3-other:content", (Serializable) blob);
        doc = session.saveDocument(doc);
        txFeature.nextTransaction(); // avoid an error in FulltextExtractorWork -> ManagedBlob

        blob = (ManagedBlob & Serializable) doc.getPropertyValue("s3-other:content");
        String digest = blob.getDigest(); // digest is the key
        assertEquals("other", blob.getProviderId());
        assertEquals("other:" + digest, blob.getKey());
        assertEquals(blobContent, blob.getString());
    }

}
