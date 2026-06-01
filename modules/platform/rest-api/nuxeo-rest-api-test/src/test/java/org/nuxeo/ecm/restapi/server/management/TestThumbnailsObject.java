/*
 * (C) Copyright 2019-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour Al Kotob
 *     Guillaume Renard
 */
package org.nuxeo.ecm.restapi.server.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_MESSAGE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_HAS_ERROR;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_PROCESSED;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_TOTAL;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailFeature;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 11.3
 */
@Features({ AutomationFeature.class, ThumbnailFeature.class })
public class TestThumbnailsObject extends ManagementBaseTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected ThumbnailService thumbnailService;

    protected DocumentRef docRef;

    @Before
    public void createDocument() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"), "image/jpeg",
                StandardCharsets.UTF_8.name(), "test.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc.putContextData(ThumbnailConstants.DISABLE_THUMBNAIL_COMPUTATION, true);
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        docRef = doc.getRef();
    }

    @Test
    public void testRecomputeThumbnailsNoQuery() {
        doTestRecomputeThumbnails(null, true);
    }

    @Test
    public void testRecomputeThumbnailsValidQuery() {
        String query = "SELECT * FROM Document WHERE ecm:mixinType = 'Thumbnail'";
        doTestRecomputeThumbnails(query, true);
    }

    @Test
    public void testRecomputeThumbnailsInvalidQuery() {
        String query = "SELECT * FROM nowhere";
        doTestRecomputeThumbnails(query, false);
    }

    /** @since 2025.22 */
    @Test
    public void testRemoveThumbnailsNoQuery() {
        doTestRemoveThumbnails(null, true);
    }

    /** @since 2025.22 */
    @Test
    public void testRemoveThumbnailsValidQuery() {
        String query = "SELECT * FROM Document WHERE ecm:mixinType = 'Thumbnail'";
        doTestRemoveThumbnails(query, true);
    }

    /** @since 2025.22 */
    @Test
    public void testRemoveThumbnailsInvalidQuery() {
        String query = "SELECT * FROM nowhere";
        doTestRemoveThumbnails(query, false);
    }

    protected void doTestRecomputeThumbnails(String query, boolean success) {
        // generating new thumbnails
        var requestBuilder = httpClient.buildPostRequest("/management/thumbnails/recompute")
                                       .contentType("application/x-www-form-urlencoded");
        if (query != null) {
            requestBuilder.entity(Map.of("query", query));
        }
        String commandId = requestBuilder.executeAndThen(new JsonNodeHandler(), node -> {
            assertBulkStatusScheduled(node);
            return getBulkCommandId(node);
        });

        // waiting for the asynchronous thumbnails recompute task
        txFeature.nextTransaction();

        httpClient.buildGetRequest("/management/bulk/" + commandId).executeAndConsume(new JsonNodeHandler(), node -> {
            assertBulkStatusCompleted(node);
            if (success) {
                assertEquals(1, node.get(STATUS_PROCESSED).asInt());
                assertFalse(node.get(STATUS_HAS_ERROR).asBoolean());
                assertEquals(0, node.get(STATUS_ERROR_COUNT).asInt());
                assertEquals(1, node.get(STATUS_TOTAL).asInt());
            } else {
                assertEquals(0, node.get(STATUS_PROCESSED).asInt());
                assertTrue(node.get(STATUS_HAS_ERROR).asBoolean());
                assertEquals(1, node.get(STATUS_ERROR_COUNT).asInt());
                assertEquals(0, node.get(STATUS_TOTAL).asInt());
                assertEquals("Invalid query", node.get(STATUS_ERROR_MESSAGE).asText());
            }
        });

        DocumentModel doc = session.getDocument(docRef);
        Blob thumbnail = thumbnailService.getThumbnail(doc, session);
        assertEquals(success, thumbnail != null);
    }

    /** @since 2025.22 */
    protected void doTestRemoveThumbnails(String query, boolean success) {
        // first generate a thumbnail so there is something to remove
        // (the @Before created the doc with DISABLE_THUMBNAIL_COMPUTATION)
        doTestRecomputeThumbnails(null, true);
        DocumentModel doc = session.getDocument(docRef);
        assertTrue(doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        assertNotNull(doc.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME));

        // submit the DELETE
        var path = "/management/thumbnails/remove";
        if (query != null) {
            path += "?query=" + query;
        }
        var requestBuilder = httpClient.buildDeleteRequest(path);
        String commandId = requestBuilder.executeAndThen(new JsonNodeHandler(), node -> {
            assertBulkStatusScheduled(node);
            return getBulkCommandId(node);
        });

        // waiting for the asynchronous thumbnails remove task
        txFeature.nextTransaction();

        httpClient.buildGetRequest("/management/bulk/" + commandId).executeAndConsume(new JsonNodeHandler(), node -> {
            assertBulkStatusCompleted(node);
            if (success) {
                assertEquals(1, node.get(STATUS_PROCESSED).asInt());
                assertFalse(node.get(STATUS_HAS_ERROR).asBoolean());
                assertEquals(0, node.get(STATUS_ERROR_COUNT).asInt());
                assertEquals(1, node.get(STATUS_TOTAL).asInt());
            } else {
                assertEquals(0, node.get(STATUS_PROCESSED).asInt());
                assertTrue(node.get(STATUS_HAS_ERROR).asBoolean());
                assertEquals(1, node.get(STATUS_ERROR_COUNT).asInt());
                assertEquals(0, node.get(STATUS_TOTAL).asInt());
                assertEquals("Invalid query", node.get(STATUS_ERROR_MESSAGE).asText());
            }
        });

        doc = session.getDocument(docRef);
        if (success) {
            assertFalse(doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
            assertNull(thumbnailService.getThumbnail(doc, session));
        } else {
            assertTrue(doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
            assertNotNull(doc.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME));
        }
    }

}
