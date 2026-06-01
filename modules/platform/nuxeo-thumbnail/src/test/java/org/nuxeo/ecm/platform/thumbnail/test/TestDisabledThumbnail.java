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
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.thumbnail.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.22
 */
@RunWith(FeaturesRunner.class)
@Features(ThumbnailFeature.class)
@Deploy("org.nuxeo.ecm.platform.thumbnail.test:test-thumbnail-listener-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.thumbnail.test:test-thumbnail-disabled-contrib.xml")
public class TestDisabledThumbnail {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void resetCounter() {
        UpdateThumbnailCounter.count = 0;
    }

    protected Blob jpegBlob() throws IOException {
        return Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/big_nuxeo_logo.jpg"), "image/jpeg");
    }

    @Test
    public void testCreationDoesNotGenerateThumbnail() throws IOException {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file.setPropertyValue("file:content", (Serializable) jpegBlob());
        file = session.createDocument(file);

        txFeature.nextTransaction();

        file = session.getDocument(file.getRef());
        assertFalse("Disabled mode must not auto-generate at creation",
                file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        assertEquals(0, UpdateThumbnailCounter.count);
    }

    @Test
    public void testUpdateWithoutFacetDoesNothing() throws IOException {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        txFeature.nextTransaction();

        file.setPropertyValue("file:content", (Serializable) jpegBlob());
        file = session.saveDocument(file);
        txFeature.nextTransaction();

        file = session.getDocument(file.getRef());
        assertFalse("Disabled mode must not auto-generate at update when no prior thumbnail",
                file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        assertEquals(0, UpdateThumbnailCounter.count);
    }

    @Test
    public void testUpdateWithFacetRefreshesThumbnail() throws IOException {
        // simulate a doc that already has a thumbnail (e.g. created before thumbnail was disabled)
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
        file.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME,
                (Serializable) Blobs.createBlob("dummy", "image/jpeg"));
        file = session.createDocument(file);
        txFeature.nextTransaction();
        UpdateThumbnailCounter.count = 0;

        // now update the main content
        file.setPropertyValue("file:content", (Serializable) jpegBlob());
        file = session.saveDocument(file);
        txFeature.nextTransaction();

        file = session.getDocument(file.getRef());
        assertTrue(file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        assertEquals("Disabled mode must refresh existing thumbnails on update", 1, UpdateThumbnailCounter.count);
    }

}
