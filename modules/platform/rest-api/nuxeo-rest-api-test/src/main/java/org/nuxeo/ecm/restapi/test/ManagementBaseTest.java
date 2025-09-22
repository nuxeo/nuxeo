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

package org.nuxeo.ecm.restapi.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_COMMAND_ID;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import javax.inject.Inject;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 11.3
 */
@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
public abstract class ManagementBaseTest {

    protected static final Set<String> SCHEDULED_STATES = Set.of(BulkStatus.State.SCHEDULED.name(),
            BulkStatus.State.SCROLLING_RUNNING.name(), BulkStatus.State.RUNNING.name());

    @Inject
    protected RestServerFeature restServerFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.defaultClient(
            () -> restServerFeature.getRestApiUrl());

    protected void assertJsonResponse(String actual, String expectedFile) throws IOException, JSONException {
        File file = FileUtils.getResourceFileFromContext(expectedFile);
        String expected = readFileToString(file, UTF_8);
        JSONAssert.assertEquals(expected, actual, true);
    }

    protected String getBulkCommandId(JsonNode bulkStatus) {
        return bulkStatus.get(STATUS_COMMAND_ID).asText();
    }

    protected void assertBulkStatusScheduled(JsonNode bulkStatus) {
        String actualState = bulkStatus.get("state").asText();
        assertTrue("Unexpected state: " + actualState, SCHEDULED_STATES.contains(actualState));
    }

    protected void assertBulkStatusCompleted(JsonNode bulkStatus) {
        assertEquals(bulkStatus.toString(), BulkStatus.State.COMPLETED.name(), bulkStatus.get("state").asText());
        Instant completed = Instant.parse(bulkStatus.get("completed").asText());
        assertTrue(completed.isBefore(Instant.now()));
        assertNotNull(bulkStatus.get("processingMillis"));
    }
}
