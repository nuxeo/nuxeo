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
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.FETCH_PROPERTIES;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.GroupConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

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
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/user/user1")) {

            // Then it returns the Json
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());

            assertEqualsUser("user1", "John", "Lennon", node);
        }
    }

    @Test
    public void itReturnsA404OnNonExistentUser() throws Exception {
        // Given a non existent user

        // When I call the Rest endpoint
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/user/nonexistentuser")) {

            // Then it returns the Json
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void itCanUpdateAUser() throws Exception {
        // Given a modified user
        NuxeoPrincipal user = um.getPrincipal("user1");
        user.setFirstName("Paul");
        user.setLastName("McCartney");
        String userJson = getPrincipalAsJson(user);

        // When I call a PUT on the Rest endpoint
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/user/user1", userJson)) {

            // Then it changes the user
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEqualsUser("user1", "Paul", "McCartney", node);

            nextTransaction(); // see committed changes
            user = um.getPrincipal("user1");
            assertEquals("Paul", user.getFirstName());
            assertEquals("McCartney", user.getLastName());
        }
    }

    @Test
    public void itCanDeleteAUser() throws Exception {
        // Given a modified user
        NuxeoPrincipal user = um.getPrincipal("user1");

        // When I call a DELETE on the Rest endpoint
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/user/user1")) {

            // Then the user is deleted
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

            nextTransaction(); // see committed changes
            user = um.getPrincipal("user1");
            assertNull(user);
        }
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
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/user", getPrincipalAsJson(principal))) {

            // Then a user is created
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEqualsUser("newuser", "test", "user", node);

            principal = um.getPrincipal("newuser");
            assertEquals("test", principal.getFirstName());
            assertEquals("user", principal.getLastName());
            assertEquals("nuxeo", principal.getCompany());
            assertEquals("test@nuxeo.com", principal.getEmail());
        }

        um.deleteUser("newuser");
        assertNull(um.getPrincipal("newuser"));
    }

    @Test
    public void itCanGetAGroup() throws Exception {
        // Given a group
        NuxeoGroup group = um.getGroup("group1");

        // When i GET on the API
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/group/" + group.getName())) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Then i GET the Group
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(5, node.size());
            assertEqualsGroup(group.getName(), group.getLabel(), node);
        }
    }

    @Test
    public void itCanGetAGroupWithFetchProperties() throws Exception {
        NuxeoGroup group = new NuxeoGroupImpl("newGroup");
        group.setLabel("a new group");
        group.setMemberUsers(Arrays.asList("user1", "user2"));
        group.setMemberGroups(Collections.singletonList("group2"));
        group.setParentGroups(Collections.singletonList("supergroup"));
        um.createGroup(group.getModel());
        nextTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle(FETCH_PROPERTIES + "." + NuxeoGroupJsonWriter.ENTITY_TYPE,
                "memberUsers,memberGroups,parentGroups");
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/group/" + group.getName(),
                queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(8, node.size());
            JsonNode memberUsers = node.get("memberUsers");
            assertTrue(memberUsers.isArray());
            assertEquals(2, memberUsers.size());
            assertEquals("user1", memberUsers.get(0).getValueAsText());
            assertEquals("user2", memberUsers.get(1).getValueAsText());
            JsonNode memberGroups = node.get("memberGroups");
            assertEquals(1, memberGroups.size());
            assertEquals("group2", memberGroups.get(0).getValueAsText());
            JsonNode parentGroups = node.get("parentGroups");
            assertEquals(1, parentGroups.size());
            assertEquals("supergroup", parentGroups.get(0).getValueAsText());
        } finally {
            um.deleteGroup(group.getModel());
        }
    }

    @Test
    public void itCanChangeAGroup() throws Exception {
        // Given a modified group
        NuxeoGroup group = um.getGroup("group1");
        group.setLabel("modifiedGroup");
        group.setMemberUsers(Arrays.asList(new String[] { "user1", "user2" }));
        group.setMemberGroups(Arrays.asList(new String[] { "group2" }));
        GroupConfig groupConfig = um.getGroupConfig();
        group.getModel().setProperty(groupConfig.schemaName, "description", "updated description");

        // When i PUT this group
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/group/" + group.getName(),
                getGroupAsJson(group))) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Then the group is modified server side
            nextTransaction(); // see committed changes
            group = um.getGroup("group1");
            assertEquals("modifiedGroup", group.getLabel());
            assertEquals("updated description", group.getModel().getProperty(groupConfig.schemaName, "description"));
            assertEquals(2, group.getMemberUsers().size());
            assertEquals(1, group.getMemberGroups().size());
        }
    }

    /**
     * @since 9.3
     */
    @Test
    public void itCanChangeAGroupWithMissingProperties() throws Exception {
        // Given a group with properties, members, subgroups and parent groups
        DocumentModel groupModel = um.getGroupModel("group1");
        GroupConfig groupConfig = um.getGroupConfig();
        groupModel.setProperty(groupConfig.schemaName, "description", "Initial description");
        groupModel.setProperty(groupConfig.schemaName, "members", Arrays.asList(new String[] { "user1", "user2" }));
        groupModel.setProperty(groupConfig.schemaName, "subGroups", Arrays.asList(new String[] { "group2" }));
        groupModel.setProperty(groupConfig.schemaName, "parentGroups", Arrays.asList(new String[] { "group3" }));
        um.updateGroup(groupModel);
        nextTransaction();

        // When I PUT this group including the properties field but omitting the memberUsers, memberGroups and
        // parentGroups fields in the JSON object
        StringBuilder groupAsJSON = new StringBuilder();
        groupAsJSON.append("{");
        groupAsJSON.append("  \"entity-type\": \"group\",");
        groupAsJSON.append("  \"id\": \"group1\",");
        groupAsJSON.append("  \"properties\": {");
        groupAsJSON.append("    \"description\": \"Updated description\"");
        groupAsJSON.append("  }");
        groupAsJSON.append("}");
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/group/group1", groupAsJSON.toString())) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Then the group properties are updated server side and the members, subgroups and parent groups are
            // unchanged
            nextTransaction(); // see committed changes
            NuxeoGroup group = um.getGroup("group1");
            assertEquals("Updated description", group.getModel().getProperty(groupConfig.schemaName, "description"));
            assertEquals(2, group.getMemberUsers().size());
            assertEquals(1, group.getMemberGroups().size());
            assertEquals(1, group.getParentGroups().size());
        }

        // When I PUT this group including the memberUsers field but omitting the properties field in the JSON object
        groupAsJSON = new StringBuilder();
        groupAsJSON.append("{");
        groupAsJSON.append("  \"entity-type\": \"group\",");
        groupAsJSON.append("  \"id\": \"group1\",");
        groupAsJSON.append("  \"memberUsers\": [\"user1\", \"user2\", \"user3\"]");
        groupAsJSON.append("}");
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/group/group1", groupAsJSON.toString())) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Then the group members are updated server side and the properties, subgroups and parent groups are
            // unchanged
            nextTransaction(); // see committed changes
            NuxeoGroup group = um.getGroup("group1");
            assertEquals(3, group.getMemberUsers().size());
            assertEquals("Updated description", group.getModel().getProperty(groupConfig.schemaName, "description"));
            assertEquals(1, group.getMemberGroups().size());
            assertEquals(1, group.getParentGroups().size());
        }
    }

    @Test
    public void itCanChangeAGroupWithCompatibilityFields() {
        String modifiedGroup = "{\n" //
                + "  \"entity-type\": \"group\",\n" //
                + "  \"id\": \"group1bis\",\n" //
                + "  \"groupname\": \"group1\",\n" //
                + "  \"grouplabel\": \"modified label\",\n" //
                + "  \"properties\": {\n" //
                + "    \"groupname\": \"group1bis\",\n" //
                + "    \"description\": \"modified description\"\n" //
                + "  }\n" + //
                "}";
        changeGroupWithCompatibilityFields("group1", modifiedGroup, "modified label");

        modifiedGroup = "{\n" //
                + "  \"entity-type\": \"group\",\n" //
                + "  \"id\": \"group1bis\",\n" //
                + "  \"groupname\": \"group1\",\n" //
                + "  \"grouplabel\": \"label\",\n" //
                + "  \"properties\": {\n" //
                + "    \"groupname\": \"group1bis\",\n" //
                + "    \"grouplabel\": \"modified label\",\n" //
                + "    \"description\": \"modified description\"\n" //
                + "  }\n" + //
                "}";
        changeGroupWithCompatibilityFields("group1", modifiedGroup, "label");

        modifiedGroup = "{\n" //
                + "  \"entity-type\": \"group\",\n" //
                + "  \"id\": \"group1\",\n" //
                + "  \"grouplabel\": \"label\",\n" //
                + "  \"properties\": {\n" //
                + "    \"groupname\": \"group1bis\",\n" //
                + "    \"grouplabel\": \"modified label\",\n" //
                + "    \"description\": \"modified description\"\n" //
                + "  }\n" + //
                "}";
        changeGroupWithCompatibilityFields("group1", modifiedGroup, "modified label");

        modifiedGroup = "{\n" //
                + "  \"entity-type\": \"group\",\n" //
                + "  \"groupname\": \"group1\",\n" //
                + "  \"grouplabel\": \"new label\",\n" //
                + "  \"properties\": {\n" //
                + "    \"groupname\": \"group1bis\",\n" //
                + "    \"description\": \"modified description\"\n" //
                + "  }\n" + //
                "}";
        changeGroupWithCompatibilityFields("group1", modifiedGroup, "new label");
    }

    protected void changeGroupWithCompatibilityFields(String groupName, String groupJSON, String expectedLabel) {
        GroupConfig groupConfig = um.getGroupConfig();
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/group/" + groupName, groupJSON)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertNull(um.getGroup("group1bis"));
            NuxeoGroup group = um.getGroup("group1");
            assertEquals("group1", group.getName());
            assertEquals(expectedLabel, group.getLabel());
            assertEquals("modified description",
                    um.getGroupModel("group1").getProperty(groupConfig.schemaName, "description"));
        }
    }

    @Test
    public void itCanDeleteGroup() throws Exception {

        // When i DELETE on a group resources
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/group/group1")) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

            // Then the group is deleted
            assertNull(um.getGroup("group1"));
        }
    }

    @Test
    public void itCanCreateAGroup() throws Exception {
        // Given a modified group
        NuxeoGroup group = new NuxeoGroupImpl("newGroup");
        group.setLabel("a new group");
        group.setMemberUsers(Arrays.asList(new String[] { "user1", "user2" }));
        group.setMemberGroups(Arrays.asList(new String[] { "group2" }));
        GroupConfig groupConfig = um.getGroupConfig();
        group.getModel().setProperty(groupConfig.schemaName, "description", "new description");

        // When i POST this group
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/group/", getGroupAsJson(group))) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            // Then the group is modified server side
            group = um.getGroup("newGroup");
            assertEquals("a new group", group.getLabel());
            assertEquals("new description", group.getModel().getProperty(groupConfig.schemaName, "description"));
            assertEquals(2, group.getMemberUsers().size());
            assertEquals(1, group.getMemberGroups().size());
        }

        um.deleteGroup("newGroup");
        assertNull(um.getGroup("newGroup"));
    }

    @Test
    public void itCanCreateAGroupWithCompatibilityFields() {
        String newGroup = "{\n" //
                + "  \"entity-type\": \"group\",\n" //
                + "  \"id\": \"newgroup\",\n" //
                + "  \"groupname\": \"newgroupcompat\",\n" //
                + "  \"grouplabel\": \"a new compatibility group\",\n" //
                + "  \"properties\": {\n" //
                + "    \"groupname\": \"newgroup\",\n" //
                + "    \"grouplabel\": \"new group\"\n" //
                + "  }\n" + //
                "}";
        createGroupWithCompatibilityFields(newGroup);

        newGroup = "{\n" //
                + "  \"entity-type\": \"group\",\n" //
                + "  \"id\": \"newgroup\",\n" //
                + "  \"groupname\": \"newgroupcompat\",\n" //
                + "  \"properties\": {\n" //
                + "    \"groupname\": \"newgroup\",\n" //
                + "    \"grouplabel\": \"a new compatibility group\"\n" //
                + "  }\n" + //
                "}";
        createGroupWithCompatibilityFields(newGroup);

        newGroup = "{\n" //
                + "  \"entity-type\": \"group\",\n" //
                + "  \"groupname\": \"newgroupcompat\",\n" //
                + "  \"grouplabel\": \"a new compatibility group\",\n" //
                + "  \"properties\": {\n" //
                + "    \"groupname\": \"newgroup\"\n" //
                + "  }\n" + //
                "}";
        createGroupWithCompatibilityFields(newGroup);

        newGroup = "{\n" //
                + "  \"entity-type\": \"group\",\n" //
                + "  \"grouplabel\": \"a new compatibility group\",\n" //
                + "  \"properties\": {\n" //
                + "    \"groupname\": \"newgroupcompat\"\n" //
                + "  }\n" + //
                "}";
        createGroupWithCompatibilityFields(newGroup);
    }

    protected void createGroupWithCompatibilityFields(String groupJSON) {
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/group/", groupJSON)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            assertNull(um.getGroup("newgroup"));
            NuxeoGroup group = um.getGroup("newgroupcompat");
            assertEquals("newgroupcompat", group.getName());
            assertEquals("a new compatibility group", group.getLabel());
            um.deleteGroup("newgroupcompat");
            assertNull(um.getGroup("newgroupcompat"));
            nextTransaction(); // see committed changes
        }
    }

    @Test
    public void itCanAddAGroupToAUser() {
        checkCanAddAGroupToAUser("user1", "group2");
        checkCanAddAGroupToAUser("foouser", "group2");
    }

    protected void checkCanAddAGroupToAUser(String username, String groupname) {
        NuxeoPrincipal principal = um.getPrincipal(username);
        NuxeoGroup group = um.getGroup(groupname);
        assertFalse(principal.isMemberOf(group.getName()));

        // When i POST this group
        try (CloseableClientResponse response = getResponse(RequestType.POST,
                "/user/" + principal.getName() + "/group/" + group.getName())) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            nextTransaction(); // see committed changes
            principal = um.getPrincipal(principal.getName());
            assertTrue(principal.isMemberOf(group.getName()));
        }
    }

    @Test
    public void itCanAddAUserToAGroup() {
        checkCanAddAUserToAGroup("group2");
        checkCanAddAUserToAGroup("foogroup");
    }

    @Test
    public void itCanAddAUserToAGroupWithSlash() {
        NuxeoGroup group = new NuxeoGroupImpl("test/slash");
        group.setLabel("a group with a slash in the name");
        um.createGroup(group.getModel());
        nextTransaction();
        checkCanAddAGroupToAUser("user1", "test/slash");
        um.deleteGroup(group.getModel());
    }

    protected void checkCanAddAUserToAGroup(String groupName) {
        NuxeoPrincipal principal = um.getPrincipal("user1");
        NuxeoGroup group = um.getGroup(groupName);
        assertFalse(principal.isMemberOf(group.getName()));

        // When i POST this group
        try (CloseableClientResponse response = getResponse(RequestType.POST,
                "/group/" + group.getName() + "/user/" + principal.getName())) {

            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            nextTransaction(); // see committed changes
            principal = um.getPrincipal(principal.getName());
            assertTrue(principal.isMemberOf(group.getName()));
        }
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
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "/user/" + principal.getName() + "/group/" + group.getName())) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            principal = um.getPrincipal(principal.getName());
            assertFalse(principal.isMemberOf(group.getName()));
        }
    }

    @Test
    public void itCanSearchUsers() throws Exception {
        // Given a search string
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("q", "Steve");

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/user/search", queryParams)) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("null", node.get("errorMessage").getValueAsText());
            ArrayNode entries = (ArrayNode) node.get("entries");
            assertEquals(1, entries.size());
            assertEquals("user0", entries.get(0).get("id").getValueAsText());
        }
    }

    @Test
    public void itCanPaginateUsers() throws Exception {

        String[][] expectedPages = new String[][] { new String[] { "Administrator", "foouser", "Guest" },
                new String[] { "user0", "user1", "user2" }, new String[] { "user3", "user4" } };

        for (int i = 0; i < expectedPages.length; i++) {
            JsonNode node = getResponseAsJson(RequestType.GET, "/user/search", getQueryParamsForPage(i));
            assertPaging(i, 3, 3, 8, expectedPages[i].length, node);
            assertUserEntries(node, expectedPages[i]);

        }

    }

    @Test
    public void itCanSearchGroups() throws Exception {
        // Given a search string
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("q", "Lannister");

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/group/search", queryParams)) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("null", node.get("errorMessage").getValueAsText());
            ArrayNode entries = (ArrayNode) node.get("entries");
            assertEquals(1, entries.size());
            assertEquals("Lannister", entries.get(0).get("grouplabel").getValueAsText());
        }
    }

    @Test
    public void itCanPaginateGroups() throws Exception {

        String[][] expectedResults = new String[][] { new String[] { "administrators", "foogroup", "group0" },
                new String[] { "group1", "group2", "group3" }, new String[] { "members", "powerusers" },
                new String[0], };

        for (int i = 0; i < expectedResults.length; i++) {
            JsonNode node = getResponseAsJson(RequestType.GET, "/group/search", getQueryParamsForPage(i));
            assertPaging(i, 3, 3, 8, expectedResults[i].length, node);
            assertGroupEntries(node, expectedResults[i]);

        }

    }

    /**
     * @since 8.2
     */
    @Test
    public void itCanPaginateGroupMembers() throws Exception {

        String[][] expectedResults = new String[][] { new String[] { "dummy", "dummy", "dummy" },
                new String[] { "dummy", "foouser" } };

        for (int i = 0; i < expectedResults.length; i++) {
            JsonNode node = getResponseAsJson(RequestType.GET, "/group/group1/@users", getQueryParamsForPage(i));
            assertPaging(i, 3, 2, 5, expectedResults[i].length, node);
        }

    }

    @Test
    public void itDoesntWritePassword() throws Exception {
        // When I call JSON for user1
        JsonNode node = getResponseAsJson(RequestType.GET, "/user/user1");

        // Then it doesn't contain the password at all
        assertNull("", node.get("properties").get("password"));

    }

    @Test
    public void itCanFetchATransientUser() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/user/transient/foo@bar.com/666")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEqualsUser("transient/foo@bar.com/666", "foo@bar.com", "null", "foo@bar.com", node);
        }
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
            assertEquals(groups[i], entries.get(i).get("id").getValueAsText());
        }
    }

}
