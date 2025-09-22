/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.quota.count;

import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.restapi.test.JsonNodeHelper.getErrorMessage;
import static org.nuxeo.ecm.restapi.test.JsonNodeHelper.getErrorStatus;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.9
 */
@RunWith(FeaturesRunner.class)
@Features({ QuotaFeature.class, RestServerFeature.class })
public class TestQuotaViaRest {

    @Inject
    protected CoreSession session;

    @Inject
    protected RestServerFeature restServerFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.defaultClient(
            () -> restServerFeature.getRestApiUrl());

    // NXP-30837
    @Test
    public void testQuotaExceedReturnsEntityTooLargeStatusCode() {
        // prepare a Workspace with a quota
        DocumentModel ws = session.createDocumentModel("/", "ws", "Workspace");
        ws = session.createDocument(ws);
        var wsQuotaAware = ws.getAdapter(QuotaAware.class);
        wsQuotaAware.setMaxQuota(100);
        wsQuotaAware.save();

        txFeature.nextTransaction();

        // upload a blob that exceeds the quota and try to create a document from it
        String batchId = httpClient.buildPostRequest("/upload")
                                   .executeAndThen(new JsonNodeHandler(SC_CREATED),
                                           node -> node.get("batchId").asText());
        httpClient.buildPostRequest("/upload/" + batchId + "/0")
                  .addHeader("X-File-Name", "long_file.txt")
                  .addHeader("X-File-Type", "text/plain")
                  .entity("a".repeat(200))
                  .execute(new JsonNodeHandler(SC_CREATED));
        httpClient.buildPostRequest("/path/ws")
                  .entity("""
                          {
                            "entity-type": "document",
                            "name": "file",
                            "type": "File",
                            "properties": {
                              "file:content": {
                                "upload-batch": "%s",
                                "upload-fileId": "0"
                              }
                            }
                          }
                          """.formatted(batchId))
                  .contentType(APPLICATION_JSON)
                  .executeAndConsume(new JsonNodeHandler(SC_REQUEST_ENTITY_TOO_LARGE), node -> {
                      assertEquals(
                              "Current event documentCreated would break Quota restriction, rolling back, QuotaExceeded",
                              getErrorMessage(node));
                      assertEquals(SC_REQUEST_ENTITY_TOO_LARGE, getErrorStatus(node));
                  });
    }
}
