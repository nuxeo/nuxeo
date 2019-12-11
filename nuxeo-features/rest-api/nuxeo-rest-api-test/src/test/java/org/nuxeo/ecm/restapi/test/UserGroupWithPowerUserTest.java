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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.sun.jersey.api.client.ClientResponse;
import org.nuxeo.runtime.test.runner.Jetty;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
@Jetty(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-usermanager-powerusers.xml")
public class UserGroupWithPowerUserTest extends BaseUserTest {

    public static final String ADMINISTRATORS_GROUP = "administrators";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected UserManager userManager;

    @Before
    public void before() {
        // power user
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", "leela");
        user.setPropertyValue("user:password", "pwd");
        user.setPropertyValue("user:groups", (Serializable) Collections.singletonList("powerusers"));
        userManager.createUser(user);

        // simple user with no group
        user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", "fry");
        userManager.createUser(user);

        NuxeoGroup group = new NuxeoGroupImpl("subgroup");
        group.setParentGroups(Collections.singletonList(ADMINISTRATORS_GROUP));
        userManager.createGroup(group.getModel());

        txFeature.nextTransaction();

        service = getServiceFor("leela", "pwd");
    }

    @Test
    public void testPowerUserCannotCreateAdministratorsGroup() throws IOException {
        NuxeoGroup group = new NuxeoGroupImpl("foo");
        String groupJson = getGroupAsJson(group);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "/group", groupJson)) {
            assertForbiddenResponseMessage("Cannot create artifact", response);
        }
    }

    @Test
    public void testPowerUserCannotUpdateAdministratorsGroup() throws IOException {
        NuxeoGroup group = userManager.getGroup(ADMINISTRATORS_GROUP);
        group.setLabel("foo");
        String groupJson = getGroupAsJson(group);

        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/group/administrators", groupJson)) {
            assertForbiddenResponseMessage("User is not allowed to edit users", response);
        }
    }

    @Test
    public void testPowerUserCannotDeleteAdministratorsGroup() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/group/administrators")) {
            assertForbiddenResponseMessage("User is not allowed to edit users", response);
        }
    }

    @Test
    public void testPowerUserCannotCreateGroupWithParentAdministratorsGroup() throws IOException {
        NuxeoGroup group = new NuxeoGroupImpl("bar");
        group.setParentGroups(Collections.singletonList(ADMINISTRATORS_GROUP));
        String groupJson = getGroupAsJson(group);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "/group", groupJson)) {
            assertForbiddenResponseMessage("Cannot create artifact", response);
        }

        // subgroup has administrators as parent group
        group = new NuxeoGroupImpl("bar");
        group.setParentGroups(Collections.singletonList("subgroup"));
        groupJson = getGroupAsJson(group);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "/group", groupJson)) {
            assertForbiddenResponseMessage("Cannot create artifact", response);
        }
    }

    @Test
    public void testPowerUserCannotCreateAdministratorUser() throws IOException {
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("bar");
        principal.setGroups(Collections.singletonList(ADMINISTRATORS_GROUP));
        String userJson = getPrincipalAsJson(principal);

        try (CloseableClientResponse response = getResponse(RequestType.POST, "/user", userJson)) {
            assertForbiddenResponseMessage("Cannot create artifact", response);
        }
    }

    @Test
    public void testPowerUserCannotUpdateAdministratorUser() throws IOException {
        NuxeoPrincipal user = userManager.getPrincipal("Administrator");
        user.setFirstName("foo");
        String userJson = getPrincipalAsJson(user);

        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/user/Administrator", userJson)) {
            assertForbiddenResponseMessage("User is not allowed to edit users", response);
        }
    }

    @Test
    public void testPowerUserCannotDeleteAdministratorUser() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/user/Administrator")) {
            assertForbiddenResponseMessage("User is not allowed to edit users", response);
        }
    }

    @Test
    public void testPowerUserCannotPromoteUserAsAdministrator() throws IOException {
        NuxeoPrincipal user = userManager.getPrincipal("fry");
        user.setGroups(Collections.singletonList(ADMINISTRATORS_GROUP));
        String userJson = getPrincipalAsJson(user);

        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/user/fry", userJson)) {
            assertForbiddenResponseMessage("User is not allowed to edit users", response);
        }

        // subgroup has administrators as parent group
        user = userManager.getPrincipal("fry");
        user.setGroups(Collections.singletonList("subgroup"));
        userJson = getPrincipalAsJson(user);

        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/user/fry", userJson)) {
            assertForbiddenResponseMessage("User is not allowed to edit users", response);
        }
    }

    @Test
    public void testPowerUserCannotPromoteHimselfAsAdministrator() throws IOException {
        NuxeoPrincipal user = userManager.getPrincipal("leela");
        List<String> groups = user.getGroups();
        groups.add(ADMINISTRATORS_GROUP);
        user.setGroups(groups);
        String userJson = getPrincipalAsJson(user);

        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/user/leela", userJson)) {
            assertForbiddenResponseMessage("User is not allowed to edit users", response);
        }
    }

    @Test
    public void testPowerUserCannotAddAdministratorsGroup() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/user/fry/group/administrators")) {
            assertForbiddenResponseMessage("Cannot edit user", response);
        }
    }

    @Test
    public void testPowerUserCannotRemoveAdministratorsGroup() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "/user/Administrator/group/administrators")) {
            assertForbiddenResponseMessage("Cannot edit user", response);
        }
    }

    @Test
    public void testPowerUserCannotAddUserToAdministratorsGroup() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/group/administrators/user/fry")) {
            assertForbiddenResponseMessage("Cannot edit user", response);
        }
    }

    @Test
    public void testPowerUserCannotRemoveUserFromAdministratorsGroup() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "/group/administrators/user/Administrator")) {
            assertForbiddenResponseMessage("Cannot edit user", response);
        }
    }

    protected void assertForbiddenResponseMessage(String expectedMessage, ClientResponse response) throws IOException {
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(expectedMessage, node.get("message").getTextValue());
    }
}
