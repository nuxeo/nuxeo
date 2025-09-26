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
 *     Charles Boidot
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_MESSAGE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_HAS_ERROR;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_PROCESSED;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_TOTAL;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.video.VideoFeature;
import org.nuxeo.ecm.platform.video.listener.VideoChangedListener;
import org.nuxeo.ecm.platform.video.service.VideoService;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 11.5
 */
@Features(VideoFeature.class)
@Deploy("org.nuxeo.ecm.platform.video.rest")
public class TestVideosObject extends ManagementBaseTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected VideoService videoService = Framework.getService(VideoService.class);

    protected DocumentRef docRef;

    @Before
    public void createDocument() throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "videoDoc", "Video");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("videos/video.mpg"), null,
                StandardCharsets.UTF_8.name(), "video.mpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc.putContextData(VideoChangedListener.DISABLE_VIDEO_CONVERSIONS_GENERATION_LISTENER, true);
        doc = session.createDocument(doc);
        session.save();
        docRef = doc.getRef();
        // Required so doc is visible in another tx (management endpoint)
        txFeature.nextTransaction();
    }

    @Test
    public void testRecomputeVideosInvalidQuery() {
        String query = "SELECT * FROM nowhere";
        doTestRecomputeVideos(query, null, false, false);
    }

    @Test
    public void testRecomputeVideosNoQueryNoConversions() {
        doTestRecomputeVideos(null, null, false, true);
    }

    @Test
    public void testRecomputeVideosValidQueryCustomConversion() {
        String query = "SELECT * FROM Document WHERE ecm:mixinType = 'Video'";
        doTestRecomputeVideos(query, List.of("WebM 480p"), false, true);
    }

    @Test
    public void testRecomputeVideosImpossibleConversion() {
        doTestRecomputeVideos(null, List.of("foo 480p"), true, false);
    }

    @Test
    public void testRecomputeVideosCustomRenditionsList() {
        doTestRecomputeVideos(null, List.of("WebM 480p", "MP4 480p"), false, true);
    }

    @Test
    public void testRecomputeOneAfterRecomputeAll() {
        // generating all default video renditions
        doTestRecomputeVideos(null, null, false, true);

        // try recomputing only the Ogg conversion
        String commandId = httpClient.buildPostRequest("/management/videos/recompute/")
                                     .entity(Map.of("conversionNames", "Ogg 480p"))
                                     .executeAndThen(new JsonNodeHandler(), node -> {
                                         assertBulkStatusScheduled(node);
                                         return getBulkCommandId(node);
                                     });
        // waiting for the asynchronous video renditions recompute task
        txFeature.nextTransaction();

        httpClient.buildGetRequest("/management/bulk/" + commandId)
                  .executeAndConsume(new JsonNodeHandler(), this::assertBulkStatusCompleted);

        DocumentModel doc = session.getDocument(docRef);
        @SuppressWarnings("unchecked")
        var transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(TRANSCODED_VIDEOS_PROPERTY);
        assertTranscodedVideos(null, transcodedVideos);
    }

    protected void doTestRecomputeVideos(String query, List<String> expectedRenditions,
            boolean expectMissingConversionError, boolean expectSuccess) {
        // Test there is no already generated renditions
        DocumentModel doc = session.getDocument(docRef);

        @SuppressWarnings("unchecked")
        var transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(TRANSCODED_VIDEOS_PROPERTY);
        assertTrue(transcodedVideos.isEmpty());

        // generating new video renditions
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        if (query != null) {
            formData.add("query", query);
        }
        if (expectedRenditions != null) {
            formData.put("conversionNames", expectedRenditions);
        }
        var requestBuilder = httpClient.buildPostRequest("/management/videos/recompute/").entity(formData);
        if (expectMissingConversionError) {
            requestBuilder.executeAndConsume(new HttpStatusCodeHandler(),
                    status -> assertEquals(SC_BAD_REQUEST, status.intValue()));
        } else {
            String commandId = requestBuilder.executeAndThen(new JsonNodeHandler(), node -> {
                assertBulkStatusScheduled(node);
                return getBulkCommandId(node);
            });

            // waiting for the asynchronous video renditions recompute task
            txFeature.nextTransaction();
            assertResponse(commandId, expectedRenditions, expectSuccess);
        }

    }

    protected void assertResponse(String commandId, List<String> expectedRenditions, boolean expectSuccess) {
        httpClient.buildGetRequest("/management/bulk/" + commandId).executeAndConsume(new JsonNodeHandler(), node -> {
            assertBulkStatusCompleted(node);
            // for visibility
            txFeature.nextTransaction();
            DocumentModel doc = session.getDocument(docRef);

            @SuppressWarnings("unchecked")
            var transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(TRANSCODED_VIDEOS_PROPERTY);
            assertNotNull(transcodedVideos);
            if (expectSuccess) {
                assertEquals(1, node.get(STATUS_PROCESSED).asInt());
                assertFalse(node.get(STATUS_HAS_ERROR).asBoolean());
                assertEquals(0, node.get(STATUS_ERROR_COUNT).asInt());
                assertEquals(1, node.get(STATUS_TOTAL).asInt());
                assertTranscodedVideos(expectedRenditions, transcodedVideos);
            } else {
                assertEquals(0, node.get(STATUS_PROCESSED).asInt());
                assertTrue(node.get(STATUS_HAS_ERROR).asBoolean());
                assertEquals(1, node.get(STATUS_ERROR_COUNT).asInt());
                assertEquals(0, node.get(STATUS_TOTAL).asInt());
                assertEquals("Invalid query", node.get(STATUS_ERROR_MESSAGE).asText());
                assertTrue(transcodedVideos.isEmpty());
            }
        });
    }

    protected void assertTranscodedVideos(List<String> expectedRenditions,
            List<Map<String, Serializable>> transcodedVideos) {
        if (expectedRenditions == null) {
            expectedRenditions = videoService.getAvailableVideoConversionsNames();
        }
        int nbExpectedRenditions = expectedRenditions.size();
        assertEquals(nbExpectedRenditions, transcodedVideos.size());
        for (int i = 0; i < nbExpectedRenditions; i++) {
            assertEquals(expectedRenditions.get(i), transcodedVideos.get(i).get("name"));
        }
    }

}
