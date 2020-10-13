/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_MESSAGE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_HAS_ERROR;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_PROCESSED;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_TOTAL;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

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
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 11.3
 */
@Features(AutomationFeature.class)
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.thumbnail")
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
    public void testRecomputeThumbnailsNoQuery() throws IOException {
        doTestRecomputeThumbnails(null, true);
    }

    @Test
    public void testRecomputeThumbnailsValidQuery() throws IOException {
        String query = "SELECT * FROM Document WHERE ecm:mixinType = 'Thumbnail'";
        doTestRecomputeThumbnails(query, true);
    }

    @Test
    public void testRecomputeThumbnailsInvalidQuery() throws IOException {
        String query = "SELECT * FROM nowhere";
        doTestRecomputeThumbnails(query, false);
    }

    protected void doTestRecomputeThumbnails(String query, boolean success) throws IOException {
        // generating new thumbnails
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        if (query != null) {
            formData.add("query", query);
        }
        String commandId;
        try (CloseableClientResponse response = httpClientRule.post("/management/thumbnails/recompute", formData)) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            assertBulkStatusScheduled(node);
            commandId = getBulkCommandId(node);
        }

        // waiting for the asynchronous thumbnails recompute task
        txFeature.nextTransaction();

        try (CloseableClientResponse response = httpClientRule.get("/management/bulk/" + commandId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(SC_OK, response.getStatus());

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
        }

        DocumentModel doc = session.getDocument(docRef);
        Blob thumbnail = thumbnailService.getThumbnail(doc, session);
        assertEquals(success, thumbnail != null);
    }

}
