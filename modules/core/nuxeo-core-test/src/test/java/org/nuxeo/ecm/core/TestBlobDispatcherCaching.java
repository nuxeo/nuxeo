/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-blob-dispatcher-caching.xml")
public class TestBlobDispatcherCaching {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    // NXP-33504
    @Test
    public void testDispatchToBlobProviderWithCaching() throws IOException {
        var blobContent = "A simple blob";
        var doc = session.createDocumentModel("/", "aFile", "File");
        doc.setPropertyValue("content", (Serializable) Blobs.createBlob(blobContent));
        doc = session.createDocument(doc);

        doc.setPropertyValue("dc:description", "other");
        doc = session.saveDocument(doc);
        txFeature.nextTransaction();

        var blob = (ManagedBlob) doc.getPropertyValue("content");
        var digest = blob.getDigest(); // digest is the key
        assertEquals("other", blob.getProviderId());
        assertEquals("other:" + digest, blob.getKey());
        assertEquals(blobContent, blob.getString());
    }
}
