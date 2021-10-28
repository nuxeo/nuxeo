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
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.blob.s3.S3BlobProviderFeature.S3_DOC_TYPE;

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, S3BlobProviderFeature.class })
public class TestS3BlobProviderWithDocument {

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
        var blobContent = "A simple blob";
        var doc = session.createDocumentModel("/", "document", S3_DOC_TYPE);
        doc.setPropertyValue(String.format("s3-%s:content", blobProviderId),
                (Serializable) Blobs.createBlob(blobContent));
        doc = session.createDocument(doc);
        txFeature.nextTransaction(); // avoid an error in FulltextExtractorWork -> ManagedBlob

        var blob = (SimpleManagedBlob) doc.getPropertyValue(String.format("s3-%s:content", blobProviderId));
        assertEquals(blobProviderId, blob.getProviderId());
        assertTrue(blob.getKey().startsWith(blobProviderId + ':'));
        assertEquals(blobContent, blob.getString());
    }

}
