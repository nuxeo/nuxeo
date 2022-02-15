/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@RepositoryConfig(init = RestServerInit.class)
public class UserAndGroupGuardTest extends BaseUserTest {

    @Inject
    protected UserManager um;

    @Override
    public void doBefore() {
        service = getServiceFor("user1", "user1");
        mapper = new ObjectMapper();
    }

    @Test
    public void onlyAdminCanDeleteAUser() {
        // Given a modified user

        // When I call a DELETE on the Rest endpoint
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/user/user2")) {

            // Then it returns a 403
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void onlyAdminCanUpdateAUser() throws Exception {
        NuxeoPrincipal user = um.getPrincipal("user1");

        // When i POST this group
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/user/" + user.getName(),
                getPrincipalAsJson(user))) {

            // Then it returns a 403
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void onlyAdminCanCreateAUser() throws Exception {
        // Given a new user
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("newuser");

        // When i POST it on the user endpoint
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/user", getPrincipalAsJson(principal))) {

            // Then it returns a 403
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void onlyAdminCanCreateAGroup() throws Exception {
        // Given a modified group
        NuxeoGroup group = new NuxeoGroupImpl("newGroup");

        // When i POST this group
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/group/", getGroupAsJson(group))) {

            // Then it returns a 403
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void onlyAdminCanUpdateAGroup() throws Exception {
        // Given a modified group
        NuxeoGroup group = um.getGroup("group1");

        // When i POST this group
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/group/" + group.getName(),
                getGroupAsJson(group))) {

            // Then it returns a 401
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void onlyAdminCanDeleteGroups() {
        // When i DELETE this group
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/group/group1")) {

            // Then it returns a 403
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void onlyAdminCanAddAGroupToAUser() throws Exception {
        // Given a modified group
        NuxeoGroup group = um.getGroup("group1");
        NuxeoPrincipal principal = um.getPrincipal("user1");

        // When i POST this group
        try (CloseableClientResponse response = getResponse(RequestType.POST,
                "/group/" + group.getName() + "/user/" + principal.getName(), getGroupAsJson(group))) {

            // Then it returns a 403
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void onlyAdminCanRemoveAGroupFromAUser() {
        // Given a modified group
        NuxeoGroup group = um.getGroup("group1");
        NuxeoPrincipal principal = um.getPrincipal("user1");

        // When i DELETE this group
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "/group/" + group.getName() + "/user/" + principal.getName())) {

            // Then it returns a 403
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void powerUserCantDeleteAdminArtifacts() {
        // Given a power user
        NuxeoPrincipal principal = RestServerInit.getPowerUser();
        service = getServiceFor(principal.getName(), principal.getName());

        // When i try to delete admin user
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/user/Administrator")) {
            // Then it returns a 403
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // When i try to delete admin user
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/group/administrators")) {
            // Then it returns a 403
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void powerUserCanDeleteNonAdminArtifacts() {
        // Given a power user
        NuxeoPrincipal principal = RestServerInit.getPowerUser();
        service = getServiceFor(principal.getName(), principal.getName());

        // When i try to delete admin user
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/user/user2")) {
            // Then it returns a NO_CONTENT response
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

            assertNull(um.getPrincipal("user2"));
        }
    }

}
