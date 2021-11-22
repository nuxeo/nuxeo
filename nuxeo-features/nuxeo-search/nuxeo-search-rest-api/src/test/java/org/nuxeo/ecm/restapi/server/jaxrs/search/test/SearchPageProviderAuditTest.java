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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.search.test;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.audit.provider.LatestCreatedUsersOrGroupsPageProvider.LATEST_CREATED_USERS_OR_GROUPS_PROVIDER;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.10-HF55
 */
@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, SearchRestFeature.class })
public class SearchPageProviderAuditTest extends BaseTest {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected UserManager userManager;

    @Test
    @Deploy("org.nuxeo.ecm.platform.audit")
    public void iCanPerformPageProviderOnAudit() throws Exception {
        // Request the PageProvider while there's nothing to return
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "search/pp/" + LATEST_CREATED_USERS_OR_GROUPS_PROVIDER + "/execute")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> entries = getLogEntries(node);
            assertTrue(entries.isEmpty());
        }

        // Then create some data
        DocumentModel groupModel = userManager.getBareGroupModel();
        groupModel.setProperty("group", "groupname", "my_group");
        groupModel.setProperty("group", "grouplabel", "My Group");
        groupModel.setProperty("group", "description", "description of my_group");
        userManager.createGroup(groupModel);

        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", "my_user");
        userModel.setProperty("user", "firstName", "My");
        userModel.setProperty("user", "lastName", "User");
        userModel.setProperty("user", "password", "my_user");
        userManager.createUser(userModel);

        txFeature.nextTransaction();

        // Then request the data
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "search/pp/" + LATEST_CREATED_USERS_OR_GROUPS_PROVIDER + "/execute", singletonMap("properties", "*"))) {

            // Then I get user & group as document listing
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> entries = getLogEntries(node);
            assertEquals(2, entries.size());
            JsonNode jsonNode = entries.get(0);
            assertEquals("my_user", jsonNode.get("title").asText());
            assertEquals("My", jsonNode.get("properties").get("user:firstName").asText());
            assertEquals("User", jsonNode.get("properties").get("user:lastName").asText());
            jsonNode = entries.get(1);
            assertEquals("my_group", jsonNode.get("title").asText());
            assertEquals("My Group", jsonNode.get("properties").get("group:grouplabel").asText());
            assertEquals("description of my_group", jsonNode.get("properties").get("group:description").asText());
        }
    }
}
