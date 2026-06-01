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
package org.nuxeo.ecm.platform.thumbnail.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.storage.mongodb.IgnoreIfNotDBSMongoDBRepository;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailFeature;
import org.nuxeo.ecm.platform.thumbnail.action.RemoveThumbnailsAction;
import org.nuxeo.ecm.platform.thumbnail.listener.ThumbnailHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests the {@link RemoveThumbnailsAction} bulk action.
 *
 * @since 2025.22
 */
@RunWith(FeaturesRunner.class)
@Features(ThumbnailFeature.class)
public class TestRemoveThumbnails {

    protected static final String THUMBNAIL_QUERY = "SELECT * FROM Document WHERE ecm:mixinType = 'Thumbnail'";

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected ThumbnailService thumbnailService;

    protected DocumentModel doc;

    @Before
    public void setup() throws IOException {
        // create a document with a content that triggers thumbnail generation
        doc = session.createDocumentModel("/", "testDoc", "File");
        var blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/big_nuxeo_logo.jpg"), "image/jpeg",
                StandardCharsets.UTF_8.name(), "big_nuxeo_logo.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // wait for thumbnail generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        assertTrue(doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        assertNotNull(doc.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME));
        Blob thumbnail = thumbnailService.getThumbnail(doc, session);
        assertNotNull(thumbnail);
    }

    @Test
    public void testRemoveThumbnailsAction() throws InterruptedException {
        // submit the bulk action to remove thumbnails matching the query
        var commandId = bulkService.submit(new BulkCommand.Builder(RemoveThumbnailsAction.ACTION_NAME, THUMBNAIL_QUERY,
                SYSTEM_USERNAME).repository(session.getRepositoryName()).build());
        assertTrue("command timeout", bulkService.await(commandId, Duration.ofSeconds(60)));

        var status = bulkService.getStatus(commandId);
        assertEquals(BulkStatus.State.COMPLETED, status.getState());
        assertFalse(status.hasError());
        assertEquals(1, status.getTotal());
        assertEquals(1, status.getProcessed());

        // verify the thumbnail has been removed
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        assertFalse(doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        assertNull(thumbnailService.getThumbnail(doc, session));
    }

    @Test
    @ConditionalIgnore(condition = IgnoreIfNotDBSMongoDBRepository.class, cause = "MongoDB feature only")
    public void testThumbnailGC() {
        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME);
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob.getProviderId());
        assertNotNull(blobProvider.getFile(blob));
        ThumbnailHelper.INSTANCE.removeThumbnail(session, doc);
        txFeature.nextTransaction();
        assertNull(blobProvider.getFile(blob));
    }

}
