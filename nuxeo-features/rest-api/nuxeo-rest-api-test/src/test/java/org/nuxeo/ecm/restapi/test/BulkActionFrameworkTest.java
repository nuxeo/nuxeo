/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.time.Duration;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.action.SetPropertiesAction;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreBulkFeature.class, RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class BulkActionFrameworkTest extends BaseTest {

    @Inject
    protected BulkService bulkService;

    @Test
    public void testGetBulkStatus() throws Exception {
        // submit a bulk command to get its status
        BulkCommand command = new BulkCommand.Builder(SetPropertiesAction.ACTION_NAME,
                "SELECT * FROM Document WHERE ecm:isVersion = 0").user(session.getPrincipal().getName())
                                                                 .repository(session.getRepositoryName())
                                                                 .param("dc:description", "new description")
                                                                 .build();
        String commandId = bulkService.submit(command);
        assertTrue("Bulk action didn't finish", bulkService.await(commandId, Duration.ofSeconds(10)));

        // compute some variable to assert
        long count = session.query("SELECT * FROM Document WHERE ecm:isVersion = 0", null, 1, 0, true).totalSize();

        try (CloseableClientResponse response = getResponse(RequestType.GET, "bulk/" + commandId, null, null, null,
                null)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(count, node.get("total").asLong());
            assertEquals(count, node.get("processed").asLong());
            assertEquals(COMPLETED.name(), node.get("state").asText());
        }
    }

    @Test
    public void testGetNonExistingBulkStatus() throws Exception {
        UUID commandId = UUID.randomUUID();
        try (CloseableClientResponse response = getResponse(RequestType.GET, "bulk/" + commandId, null, null, null,
                null)) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("Bulk command with id=" + commandId + " doesn't exist", node.get("message").asText());
        }
    }

}
