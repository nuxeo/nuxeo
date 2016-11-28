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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Tests the users and groups Rest endpoints
 *
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
public class UserGroupTest extends BaseUserTest {

    @Inject
    UserManager um;

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void itCanFetchAUser() throws Exception {
        // Given the user1

        // When I call the Rest endpoint
        ClientResponse response = getResponse(RequestType.GET, "/user/user1");

        // Then it returns the Json
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());

        assertEqualsUser("user1", "John", "Lennon", node);

    }

    @Test
    public void itReturnsA404OnNonExistentUser() throws Exception {
        // Given a non existent user

        // When I call the Rest endpoint
        ClientResponse response = getResponse(RequestType.GET, "/user/nonexistentuser");

        // Then it returns the Json
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

    }

    @Test
    public void itCanUpdateAUser() throws Exception {
        // Given a modified user
        NuxeoPrincipal user = um.getPrincipal("user1");
        user.setFirstName("Paul");
        user.setLastName("McCartney");
        String userJson = getPrincipalAsJson(user);

        // When I call a PUT on the Rest endpoint
        ClientResponse response = getResponse(RequestType.PUT, "/user/user1", userJson);

        // Then it changes the user
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEqualsUser("user1", "Paul", "McCartney", node);

        nextTransaction(); // see committed changes
        user = um.getPrincipal("user1");
        assertEquals("Paul", user.getFirstName());
        assertEquals("McCartney", user.getLastName());

    }

    @Test
    public void itCanDeleteAUser() throws Exception {
        // Given a modified user
        NuxeoPrincipal user = um.getPrincipal("user1");

        // When I call a DELETE on the Rest endpoint
        ClientResponse response = getResponse(RequestType.DELETE, "/user/user1");

        // Then the user is deleted
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        nextTransaction(); // see committed changes
        user = um.getPrincipal("user1");
        assertNull(user);

    }

    @Test
    public void itCanCreateAUser() throws Exception {
        // Given a new user
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("newuser");
        principal.setFirstName("test");
        principal.setLastName("user");
        principal.setCompany("nuxeo");
        principal.setEmail("test@nuxeo.com");

        // When i POST it on the user endpoint
        ClientResponse response = getResponse(RequestType.POST, "/user", getPrincipalAsJson(principal));

        // Then a user is created
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEqualsUser("newuser", "test", "user", node);

        principal = um.getPrincipal("newuser");
        assertEquals("test", principal.getFirstName());
        assertEquals("user", principal.getLastName());
        assertEquals("nuxeo", principal.getCompany());
        assertEquals("test@nuxeo.com", principal.getEmail());

        um.deleteUser("newuser");
        assertNull(um.getPrincipal("newuser"));
    }

    @Test
    public void itCanGetAGroup() throws Exception {
        // Given a group
        NuxeoGroup group = um.getGroup("group1");

        // When i GET on the API
        ClientResponse response = getResponse(RequestType.GET, "/group/" + group.getName());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Then i GET the Group
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEqualsGroup(group.getName(), group.getLabel(), node);

    }

    @Test
    public void itCanChangeAGroup() throws Exception {
        // Given a modified group
        NuxeoGroup group = um.getGroup("group1");
        group.setLabel("modifiedGroup");
        group.setMemberUsers(Arrays.asList(new String[] { "user1", "user2" }));
        group.setMemberGroups(Arrays.asList(new String[] { "group2" }));

        // When i PUT this group
        ClientResponse response = getResponse(RequestType.PUT, "/group/" + group.getName(), getGroupAsJson(group));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Then the group is modified server side
        nextTransaction(); // see committed changes
        group = um.getGroup("group1");
        assertEquals("modifiedGroup", group.getLabel());
        assertEquals(2, group.getMemberUsers().size());
        assertEquals(1, group.getMemberGroups().size());
    }

    @Test
    public void itCanDeleteGroup() throws Exception {

        // When i DELETE on a group resources
        ClientResponse response = getResponse(RequestType.DELETE, "/group/group1");
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        // Then the group is deleted
        assertNull(um.getGroup("group1"));
    }

    @Test
    public void itCanCreateAGroup() throws Exception {
        // Given a modified group
        NuxeoGroup group = new NuxeoGroupImpl("newGroup");
        group.setLabel("a new group");
        group.setMemberUsers(Arrays.asList(new String[] { "user1", "user2" }));
        group.setMemberGroups(Arrays.asList(new String[] { "group2" }));

        // When i POST this group
        ClientResponse response = getResponse(RequestType.POST, "/group/", getGroupAsJson(group));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        // Then the group is modified server side
        group = um.getGroup("newGroup");
        assertEquals("a new group", group.getLabel());
        assertEquals(2, group.getMemberUsers().size());
        assertEquals(1, group.getMemberGroups().size());

        um.deleteGroup("newGroup");
        assertNull(um.getGroup("newGroup"));
    }

    @Test
    public void itCanAddAGroupToAUser() throws Exception {
        // Given a user and a group
        NuxeoPrincipal principal = um.getPrincipal("user1");
        NuxeoGroup group = um.getGroup("group2");
        assertFalse(principal.isMemberOf(group.getName()));

        // When i POST this group
        ClientResponse response = getResponse(RequestType.POST,
                "/user/" + principal.getName() + "/group/" + group.getName());

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        nextTransaction(); // see committed changes
        principal = um.getPrincipal(principal.getName());
        assertTrue(principal.isMemberOf(group.getName()));

    }

    @Test
    public void itCanAddAUserToAGroup() throws Exception {
        // Given a user and a group
        NuxeoPrincipal principal = um.getPrincipal("user1");
        NuxeoGroup group = um.getGroup("group2");
        assertFalse(principal.isMemberOf(group.getName()));

        // When i POST this group
        ClientResponse response = getResponse(RequestType.POST,
                "/group/" + group.getName() + "/user/" + principal.getName());

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        nextTransaction(); // see committed changes
        principal = um.getPrincipal(principal.getName());
        assertTrue(principal.isMemberOf(group.getName()));
    }

    @Test
    public void itCanRemoveAUserToAGroup() throws Exception {
        // Given a user in a group
        NuxeoPrincipal principal = um.getPrincipal("user1");
        NuxeoGroup group = um.getGroup("group1");
        principal.setGroups(Arrays.asList(new String[] { group.getName() }));
        um.updateUser(principal.getModel());
        principal = um.getPrincipal("user1");
        assertTrue(principal.isMemberOf(group.getName()));

        // commit directory changes
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // When i POST this group
        ClientResponse response = getResponse(RequestType.DELETE,
                "/user/" + principal.getName() + "/group/" + group.getName());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        principal = um.getPrincipal(principal.getName());
        assertFalse(principal.isMemberOf(group.getName()));

    }

    @Test
    public void itCanSearchUsers() throws Exception {
        // Given a search string
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("q", "Steve");

        ClientResponse response = getResponse(RequestType.GET, "/user/search", queryParams);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("null", node.get("errorMessage").getValueAsText());
        ArrayNode entries = (ArrayNode) node.get("entries");
        assertEquals(1, entries.size());
        assertEquals("user0", entries.get(0).get("id").getValueAsText());

    }

    @Test
    public void itCanPaginateUsers() throws Exception {

        String[][] expectedPages = new String[][] { new String[] { "Administrator", "Guest", "user0" },
                new String[] { "user1", "user2", "user3" }, new String[] {"user4"} };

        for (int i = 0; i < expectedPages.length; i++) {
            JsonNode node = getResponseAsJson(RequestType.GET, "/user/search", getQueryParamsForPage(i));
            assertPaging(i, 3, 3, 7, expectedPages[i].length, node);
            assertUserEntries(node, expectedPages[i]);

        }

    }

    @Test
    public void itCanSearchGroups() throws Exception {
        // Given a search string
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("q", "Lannister");

        ClientResponse response = getResponse(RequestType.GET, "/group/search", queryParams);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("null", node.get("errorMessage").getValueAsText());
        ArrayNode entries = (ArrayNode) node.get("entries");
        assertEquals(1, entries.size());
        assertEquals("Lannister", entries.get(0).get("grouplabel").getValueAsText());

    }

    @Test
    public void itCanPaginateGroups() throws Exception {

        String[][] expectedResults = new String[][] { new String[] { "administrators", "group0", "group1" },
                new String[] { "group2", "group3", "members" }, new String[] { "powerusers" }, new String[0], };

        for (int i = 0; i < expectedResults.length; i++) {
            JsonNode node = getResponseAsJson(RequestType.GET, "/group/search", getQueryParamsForPage(i));
            assertPaging(i, 3, 3, 7, expectedResults[i].length, node);
            assertGroupEntries(node, expectedResults[i]);

        }

    }

    /**
     * @since 8.2
     */
    @Test
    public void itCanPaginateGroupMembers() throws Exception {

        String[][] expectedResults = new String[][] { new String[] { "dummy", "dummy", "dummy" },
                new String[] { "dummy" }};

        for (int i = 0; i < expectedResults.length; i++) {
            JsonNode node = getResponseAsJson(RequestType.GET, "/group/group1/@users", getQueryParamsForPage(i));
            assertPaging(i, 3, 2, 4, expectedResults[i].length, node);
        }

    }

    @Test
    public void itDoesntWritePassword() throws Exception {
        // When I call JSON for user1
        JsonNode node = getResponseAsJson(RequestType.GET, "/user/user1");

        // Then it doesn't contain the password at all
        assertNull(node.get("properties").get("password"));

    }

    /**
     * @param node node to test
     * @param strings an array of expected user names
     * @since 5.8
     */
    private void assertUserEntries(JsonNode node, String... users) {
        ArrayNode entries = (ArrayNode) node.get("entries");
        assertEquals(users.length, entries.size());
        for (int i = 0; i < users.length; i++) {
            assertEquals(users[i], entries.get(i).get("id").getValueAsText());
        }
    }

    /**
     * @param currentPageIndex expected currentPage index
     * @param pageSize expected page size
     * @param numberOfPage expected number of page
     * @param resultsCount expected resultsCount
     * @param currentPageSize expected currentPageSize
     * @param jsonNodeToText
     * @since 5.8
     */
    private void assertPaging(int currentPageIndex, int pageSize, int numberOfPage, int resultsCount,
            int currentPageSize, JsonNode node) {
        assertTrue(node.get("isPaginable").getBooleanValue());
        assertEquals(currentPageIndex, node.get("currentPageIndex").getIntValue());
        assertEquals(pageSize, node.get("pageSize").getIntValue());
        assertEquals(numberOfPage, node.get("numberOfPages").getIntValue());
        assertEquals(resultsCount, node.get("resultsCount").getIntValue());
        assertEquals(currentPageSize, node.get("currentPageSize").getIntValue());
    }

    /**
     * @param pageIndex
     * @return
     * @since 5.8
     */
    private MultivaluedMap<String, String> getQueryParamsForPage(int pageIndex) {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("q", "*");
        queryParams.putSingle("currentPageIndex", Integer.toString(pageIndex));
        queryParams.putSingle("pageSize", "3");
        return queryParams;
    }

    /**
     * @param node node to test
     * @param strings an array of expected group names
     * @since 5.8
     */
    private void assertGroupEntries(JsonNode node, String... groups) {
        ArrayNode entries = (ArrayNode) node.get("entries");
        assertEquals(groups.length, entries.size());
        for (int i = 0; i < groups.length; i++) {
            assertEquals(groups[i], entries.get(i).get("groupname").getValueAsText());
        }
    }

}
