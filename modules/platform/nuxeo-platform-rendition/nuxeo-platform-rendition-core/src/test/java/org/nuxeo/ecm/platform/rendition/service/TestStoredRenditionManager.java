/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-renditionprovider-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-stored-rendition-manager-contrib.xml")
public class TestStoredRenditionManager {

    @Inject
    TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    @Inject
    protected EventService eventService;

    @Test
    public void testDummyRendition() throws Exception {
        DocumentModel file = createBlobDoc("File");
        Rendition ren = renditionService.getRendition(file, "dummyRendition", true);
        assertNotNull(ren);
        Blob blob = ren.getBlob();
        assertEquals(file.getPropertyValue("dc:description"), blob.getString());
        assertEquals("dummy/pdf", blob.getMimeType());
    }

    protected DocumentModel createBlobDoc(String typeName) {
        DocumentModel file = session.createDocumentModel("/", "dummy-file", typeName);
        file.setPropertyValue("dc:description", "dummy-description");
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        Blob blob = Blobs.createBlob("Dummy text");
        blob.setFilename("dummy.txt");
        bh.setBlob(blob);
        return session.createDocument(file);
    }

    @Test
    public void testStoredRenditionsCleanup() {
        DocumentModel file1 = createBlobDoc("File");
        DocumentModel file2 = createBlobDoc("File");

        Rendition ren1 = renditionService.getRendition(file1, "dummyRendition", true);
        assertNotNull(ren1);
        DocumentModel renditionDoc1 = ren1.getHostDocument();
        assertTrue(renditionDoc1.isVersion());
        DocumentRef liveRenditionRef1 = new IdRef(renditionDoc1.getSourceId());
        assertTrue(session.exists(liveRenditionRef1));

        Rendition ren2 = renditionService.getRendition(file2, "dummyRendition", true);
        assertNotNull(ren2);
        DocumentModel renditionDoc2 = ren2.getHostDocument();
        assertTrue(renditionDoc2.isVersion());
        DocumentRef liveRenditionRef2 = new IdRef(renditionDoc2.getSourceId());
        assertTrue(session.exists(liveRenditionRef2));

        // remove the first document (and versions)
        session.removeDocument(file1.getRef());

        txFeature.nextTransaction();

        // delete stored renditions
        renditionService.deleteStoredRenditions(session.getRepositoryName());

        txFeature.nextTransaction();

        assertFalse(session.exists(liveRenditionRef1));
        assertFalse(session.exists(renditionDoc1.getRef()));

        assertTrue(session.exists(liveRenditionRef2));
        assertTrue(session.exists(renditionDoc2.getRef()));
    }

}
