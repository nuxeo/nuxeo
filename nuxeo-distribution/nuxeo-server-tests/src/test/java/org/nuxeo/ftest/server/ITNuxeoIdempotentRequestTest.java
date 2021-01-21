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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.nuxeo.ecm.platform.web.common.idempotency.NuxeoIdempotentFilter.HEADER_KEY;
import static org.nuxeo.functionaltests.AbstractTest.NUXEO_URL;
import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.objects.Document;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * Tests for idempotent request mechanism.
 *
 * @since 11.5
 */
public class ITNuxeoIdempotentRequestTest {

    private static final String TEST_KEY = "idempotenttestkey" + new Date().getTime();

    private static final NuxeoClient.Builder CLIENT_BUILDER = new NuxeoClient.Builder().url(NUXEO_URL)
                                                                                       .authentication(ADMINISTRATOR,
                                                                                               ADMINISTRATOR)
                                                                                       .schemas("*");

    private static final NuxeoClient CLIENT = CLIENT_BUILDER.connect();

    // connect before adding the header to avoid handling auth as the idempotent request
    private static final NuxeoClient IDEMPOTENT_CLIENT = CLIENT_BUILDER.connect().header(HEADER_KEY, TEST_KEY);

    private static final String PARENT_PATH = "/default-domain/workspaces/";

    private static final String QUERY_CHILDREN = String.format("SELECT * FROM Document WHERE %s STARTSWITH '%s'",
            NXQL.ECM_PATH, PARENT_PATH);

    private static final String TEST_TITLE = "testdoc";

    private static final String TEST_TYPE = "File";

    protected String createDocument(NuxeoClient client) {
        Document document = Document.createWithName(TEST_TITLE, TEST_TYPE);
        document.setPropertyValue("dc:title", TEST_TITLE);
        Document created = client.repository().createDocumentByPath(PARENT_PATH, document);
        waitForAsyncWork();
        return created.getId();
    }

    /**
     * Prevents from random failures when counting children.
     */
    protected void waitForAsyncWork() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timeoutSecond", Integer.valueOf(110));
        parameters.put("refresh", Boolean.TRUE);
        parameters.put("waitForAudit", Boolean.TRUE);
        CLIENT.operation("Elasticsearch.WaitForIndexing").parameters(parameters).execute();
    }

    protected int getNumberOfChildren() {
        return CLIENT.repository().query(QUERY_CHILDREN).size() - 1;
    }

    protected Document fetch(String id) {
        return CLIENT.repository().fetchDocumentById(id);
    }

    protected void delete(String id) {
        CLIENT.repository().deleteDocument(id);
    }

    @Test
    public void testIdempotentRequest() {
        String id = null;
        String dupeId = null;
        try {
            assertEquals(0, getNumberOfChildren());

            // create child document
            id = createDocument(IDEMPOTENT_CLIENT);
            assertEquals(TEST_TITLE, fetch(id).getTitle());
            // doc created
            assertEquals(1, getNumberOfChildren());

            // try creating it again
            dupeId = createDocument(IDEMPOTENT_CLIENT);
            assertEquals(id, dupeId);
            // dupe not created
            assertEquals(1, getNumberOfChildren());

            // create again without idempotency key
            dupeId = createDocument(CLIENT);
            assertNotEquals(id, dupeId);
            // dupe created
            assertEquals(2, getNumberOfChildren());
        } finally {
            if (id != null) {
                delete(id);
            }
            if (dupeId != null && !dupeId.equals(id)) {
                delete(dupeId);
            }
        }
    }

}
