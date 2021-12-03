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
 *     Guillaume RENARD
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, S3BlobProviderFeature.class })
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-record.xml")
public class TestS3BlobStoreRecordWithDocument {

    @Inject
    protected CoreSession session;

    /**
     * This test has to be run against a s3 bucket with Lock Object enabled (or at least bucket versioning enabled).
     *
     * @since 2021.13
     */
    @Test
    public void testDoNotDeleteVersionedObject() {
        // Create a document with blob
        String blobContent = "A simple blob";
        DocumentModel doc = session.createDocumentModel("/", "document", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob(blobContent));
        doc = session.createDocument(doc);

        // Update original doc's blob and create version
        blobContent = "Another simple blob";
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob(blobContent));
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        session.saveDocument(doc);

        // Trigger GC and assert no binaries was garbage collected.
        BinaryManagerStatus gcSt = Framework.getService(DocumentBlobManager.class).garbageCollectBinaries(true);
        assertEquals(0, gcSt.numBinariesGC);
    }

}
