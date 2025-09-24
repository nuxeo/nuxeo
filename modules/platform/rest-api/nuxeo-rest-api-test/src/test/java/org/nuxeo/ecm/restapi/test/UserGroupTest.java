/*
 * (C) Copyright 2013-2025 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.FETCH_PROPERTIES;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Rule;
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
import org.nuxeo.ecm.restapi.server.jaxrs.usermanager.GroupRootObject;
import org.nuxeo.ecm.restapi.server.jaxrs.usermanager.UserRootObject;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Tests the users and groups Rest endpoints
 *
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
public class UserGroupTest extends BaseUserTest {

    @Inject
    protected UserManager um;

    @Inject
    protected RestServerFeature restServerFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.defaultClient(
            () -> restServerFeature.getRestApiUrl());

    @Rule
    public final HttpClientTestRule nonAdminHttpClient = HttpClientTestRule.builder()
                                                                           .url(() -> restServerFeature.getRestApiUrl())
                                                                           .credentials("user1", "user1")
                                                                           .build();

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void itCanFetchAUser() {
        // Given the user1

        // When I call the Rest endpoint
        httpClient.buildGetRequest("/user/user1")
                  .executeAndConsume(new JsonNodeHandler(),
                          // Then it returns the Json
                          node -> {
                              assertEqualsUser("user1", "John", "Lennon", node);
                              assertFalse(node.get("isAdministrator").asBoolean());
                              assertNotNull(node.get("properties"));
                              assertNotNull(node.get("properties").get("groups"));
                          });
    }

    @Test
    public void itReturnsA404OnNonExistentUser() {
        // Given a non existent user

        // When I call the Rest endpoint
        httpClient.buildGetRequest("/user/nonexistentuser")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then it returns the Json
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    @Test
    public void itCanUpdateAUser() throws IOException {
        // Given a modified user
        NuxeoPrincipal user = um.getPrincipal("user1");
        user.setFirstName("Paul");
        user.setLastName("McCartney");
        String userJson = getPrincipalAsJson(user);

        // When I call a PUT on the Rest endpoint
        httpClient.buildPutRequest("/user/user1")
                  .entity(userJson)
                  .executeAndConsume(new JsonNodeHandler(),
                          // Then it changes the user
                          node -> assertEqualsUser("user1", "Paul", "McCartney", node));

        nextTransaction(); // see committed changes
        user = um.getPrincipal("user1");
        assertEquals("Paul", user.getFirstName());
        assertEquals("McCartney", user.getLastName());
    }

    @Test
    public void itCanDeleteAUser() {
        // Given a modified user

        // When I call a DELETE on the Rest endpoint
        httpClient.buildDeleteRequest("/user/user1")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then the user is deleted
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));

        nextTransaction(); // see committed changes
        NuxeoPrincipal user = um.getPrincipal("user1");
        assertNull(user);
    }

    @Test
    public void itCanCreateAUser() {
        // Given a new user
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("newuser");
        principal.setFirstName("test");
        principal.setLastName("user");
        principal.setCompany("nuxeo");
        principal.setEmail("test@nuxeo.com");

        // When i POST it on the user endpoint
        httpClient.buildPostRequest("/user")
                  .entity(getPrincipalAsJson(principal, "testPassword"))
                  .executeAndConsume(new JsonNodeHandler(SC_CREATED),
                          // Then a user is created
                          node -> assertEqualsUser("newuser", "test", "user", node));

        principal = um.getPrincipal("newuser");
        assertEquals("test", principal.getFirstName());
        assertEquals("user", principal.getLastName());
        assertEquals("nuxeo", principal.getCompany());
        assertEquals("test@nuxeo.com", principal.getEmail());

        um.deleteUser("newuser");
        assertNull(um.getPrincipal("newuser"));
    }

    @Test
    public void itReturnsA409OnAlreadyExistentUser() throws IOException {
        // Given an existent user
        NuxeoPrincipal user1 = new NuxeoPrincipalImpl("existentuser");
        um.createUser(user1.getModel());
        nextTransaction();

        // When I try to recreate the same user and POST it on the user endpoint
        httpClient.buildPostRequest("/user")
                  .entity(getPrincipalAsJson(user1))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then it returns the JSON
                          status -> assertEquals(SC_CONFLICT, status.intValue()));
    }

    @Test
    public void itReturnsA400OnEmptyUsername() throws IOException {
        // Given a user with a null username
        NuxeoPrincipal principal = new NuxeoPrincipalImpl(null);

        // When i POST it on the user endpoint
        httpClient.buildPostRequest("/user")
                  .entity(getPrincipalAsJson(principal))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then I get a 400
                          status -> assertEquals(SC_BAD_REQUEST, status.intValue()));

        // Given a user with a blank username
        principal.setName("");

        // When i POST it on the user endpoint
        httpClient.buildPostRequest("/user")
                  .entity(getPrincipalAsJson(principal))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then I get a 400
                          status -> assertEquals(SC_BAD_REQUEST, status.intValue()));
    }

    @Test
    public void itReturnsA400OnEmptyPassword() throws IOException {
        // Given a user with a null password
        NuxeoPrincipal principal = new NuxeoPrincipalImpl("newuser");

        // When i POST it on the user endpoint
        httpClient.buildPostRequest("/user")
                  .entity(getPrincipalAsJson(principal))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then I get a 400
                          status -> assertEquals(SC_BAD_REQUEST, status.intValue()));

        // Given a user with a blank password
        // When i POST it on the user endpoint
        httpClient.buildPostRequest("/user")
                  .entity(getPrincipalAsJson(principal, ""))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then I get a 400
                          status -> assertEquals(SC_BAD_REQUEST, status.intValue()));
    }

    @Test
    @WithFrameworkProperty(name = UserRootObject.ALLOW_EMPTY_PASSWORD_PROP, value = "true")
    public void itReturnsA201OnEmptyPassword() throws IOException {
        // Given a user with an empty password
        // When i POST it on the user endpoint
        httpClient.buildPostRequest("/user")
                  .entity(getPrincipalAsJson(new NuxeoPrincipalImpl("emptyPassword")))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then I get a 201
                          status -> assertEquals(SC_CREATED, status.intValue()));

        // Given a user with a blank password
        // When i POST it on the user endpoint
        httpClient.buildPostRequest("/user")
                  .entity(getPrincipalAsJson(new NuxeoPrincipalImpl("blankPassword"), ""))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then I get a 201
                          status -> assertEquals(SC_CREATED, status.intValue()));
    }

    @Test
    public void itCanGetAGroup() {
        // Given a group
        NuxeoGroup group = um.getGroup("group1");

        // When i GET on the API
        httpClient.buildGetRequest("/group/" + group.getName()).executeAndConsume(new JsonNodeHandler(), node -> {
            // Then i GET the Group
            assertEquals(6, node.size());
            assertEqualsGroup(group.getName(), group.getLabel(), node);
        });
    }

    @Test
    public void itCanGetAGroupWithFetchProperties() {
        NuxeoGroup group = new NuxeoGroupImpl("newGroup");
        group.setLabel("a new group");
        group.setMemberUsers(List.of("user1", "user2"));
        group.setMemberGroups(List.of("group2"));
        group.setParentGroups(List.of("supergroup"));
        um.createGroup(group.getModel());
        nextTransaction();

        try {
            httpClient.buildGetRequest("/group/" + group.getName())
                      .addQueryParameter(FETCH_PROPERTIES + "." + NuxeoGroupJsonWriter.ENTITY_TYPE,
                              "memberUsers,memberGroups,parentGroups")
                      .executeAndConsume(new JsonNodeHandler(), node -> {
                          assertEquals(9, node.size());
                          JsonNode memberUsers = node.get("memberUsers");
                          assertTrue(memberUsers.isArray());
                          assertEquals(2, memberUsers.size());
                          assertEquals("user1", memberUsers.get(0).asText());
                          assertEquals("user2", memberUsers.get(1).asText());
                          JsonNode memberGroups = node.get("memberGroups");
                          assertEquals(1, memberGroups.size());
                          assertEquals("group2", memberGroups.get(0).asText());
                          JsonNode parentGroups = node.get("parentGroups");
                          assertEquals(1, parentGroups.size());
                          assertEquals("supergroup", parentGroups.get(0).asText());
                      });
        } finally {
            um.deleteGroup(group.getModel());
        }
    }

    @Test
    public void itCanChangeAGroup() throws IOException {
        // Given a modified group
        NuxeoGroup group = um.getGroup("group1");
        group.setLabel("modifiedGroup");
        group.setMemberUsers(List.of("user1", "user2"));
        group.setMemberGroups(List.of("group2"));
        GroupConfig groupConfig = um.getGroupConfig();
        group.getModel().setProperty(groupConfig.schemaName, "description", "updated description");

        // When i PUT this group
        httpClient.buildPutRequest("/group/" + group.getName())
                  .entity(getGroupAsJson(group))
                  .executeAndConsume(new HttpStatusCodeHandler(), status -> assertEquals(SC_OK, status.intValue()));

        // Then the group is modified server side
        nextTransaction(); // see committed changes
        group = um.getGroup("group1");
        assertEquals("modifiedGroup", group.getLabel());
        assertEquals("updated description", group.getModel().getProperty(groupConfig.schemaName, "description"));
        assertEquals(2, group.getMemberUsers().size());
        assertEquals(1, group.getMemberGroups().size());
    }

    /**
     * @since 9.3
     */
    @Test
    public void itCanChangeAGroupWithMissingProperties() {
        // Given a group with properties, members, subgroups and parent groups
        DocumentModel groupModel = um.getGroupModel("group1");
        GroupConfig groupConfig = um.getGroupConfig();
        groupModel.setProperty(groupConfig.schemaName, "description", "Initial description");
        groupModel.setProperty(groupConfig.schemaName, "members", List.of("user1", "user2"));
        groupModel.setProperty(groupConfig.schemaName, "subGroups", List.of("group2"));
        groupModel.setProperty(groupConfig.schemaName, "parentGroups", List.of("group3"));
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

        httpClient.buildPutRequest("/group/group1")
                  .entity(groupAsJSON.toString())
                  .executeAndConsume(new HttpStatusCodeHandler(), status -> assertEquals(SC_OK, status.intValue()));

        // Then the group properties are updated server side and the members, subgroups and parent groups are unchanged
        nextTransaction(); // see committed changes
        NuxeoGroup group = um.getGroup("group1");
        assertEquals("Updated description", group.getModel().getProperty(groupConfig.schemaName, "description"));
        assertEquals(2, group.getMemberUsers().size());
        assertEquals(1, group.getMemberGroups().size());
        assertEquals(1, group.getParentGroups().size());

        // When I PUT this group including the memberUsers field but omitting the properties field in the JSON object
        groupAsJSON = new StringBuilder();
        groupAsJSON.append("{");
        groupAsJSON.append("  \"entity-type\": \"group\",");
        groupAsJSON.append("  \"id\": \"group1\",");
        groupAsJSON.append("  \"memberUsers\": [\"user1\", \"user2\", \"user3\"]");
        groupAsJSON.append("}");
        httpClient.buildPutRequest("/group/group1")
                  .entity(groupAsJSON.toString())
                  .executeAndConsume(new HttpStatusCodeHandler(), status -> assertEquals(SC_OK, status.intValue()));

        // Then the group members are updated server side and the properties, subgroups and parent groups are unchanged
        nextTransaction(); // see committed changes
        group = um.getGroup("group1");
        assertEquals(3, group.getMemberUsers().size());
        assertEquals("Updated description", group.getModel().getProperty(groupConfig.schemaName, "description"));
        assertEquals(1, group.getMemberGroups().size());
        assertEquals(1, group.getParentGroups().size());
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
        httpClient.buildPutRequest("/group/" + groupName)
                  .entity(groupJSON)
                  .executeAndConsume(new HttpStatusCodeHandler(), status -> assertEquals(SC_OK, status.intValue()));
        assertNull(um.getGroup("group1bis"));
        NuxeoGroup group = um.getGroup("group1");
        assertEquals("group1", group.getName());
        assertEquals(expectedLabel, group.getLabel());
        assertEquals("modified description",
                um.getGroupModel("group1").getProperty(groupConfig.schemaName, "description"));
    }

    @Test
    public void itCanDeleteGroup() {

        // When i DELETE on a group resources
        httpClient.buildDeleteRequest("/group/group1")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));

        // Then the group is deleted
        assertNull(um.getGroup("group1"));
    }

    @Test
    public void itCanCreateAGroup() throws IOException {
        // Given a modified group
        NuxeoGroup group = new NuxeoGroupImpl("newGroup");
        group.setLabel("a new group");
        group.setMemberUsers(List.of("user1", "user2"));
        group.setMemberGroups(List.of("group2"));
        GroupConfig groupConfig = um.getGroupConfig();
        group.getModel().setProperty(groupConfig.schemaName, "description", "new description");

        // When i POST this group
        httpClient.buildPostRequest("/group/")
                  .entity(getGroupAsJson(group))
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_CREATED, status.intValue()));

        // Then the group is modified server side
        group = um.getGroup("newGroup");
        assertEquals("a new group", group.getLabel());
        assertEquals("new description", group.getModel().getProperty(groupConfig.schemaName, "description"));
        assertEquals(2, group.getMemberUsers().size());
        assertEquals(1, group.getMemberGroups().size());

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
        httpClient.buildPostRequest("/group/")
                  .entity(groupJSON)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_CREATED, status.intValue()));
        assertNull(um.getGroup("newgroup"));
        NuxeoGroup group = um.getGroup("newgroupcompat");
        assertEquals("newgroupcompat", group.getName());
        assertEquals("a new compatibility group", group.getLabel());
        um.deleteGroup("newgroupcompat");
        assertNull(um.getGroup("newgroupcompat"));
        nextTransaction(); // see committed changes
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
        httpClient.buildPostRequest("/user/" + principal.getName() + "/group/" + group.getName())
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_CREATED, status.intValue()));
        nextTransaction(); // see committed changes
        principal = um.getPrincipal(principal.getName());
        assertTrue(principal.isMemberOf(group.getName()));
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
        httpClient.buildPostRequest("/group/" + group.getName() + "/user/" + principal.getName())
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_CREATED, status.intValue()));

        nextTransaction(); // see committed changes
        principal = um.getPrincipal(principal.getName());
        assertTrue(principal.isMemberOf(group.getName()));
    }

    @Test
    public void itCanRemoveAUserToAGroup() {
        // Given a user in a group
        NuxeoPrincipal principal = um.getPrincipal("user1");
        NuxeoGroup group = um.getGroup("group1");
        principal.setGroups(List.of(group.getName()));
        um.updateUser(principal.getModel());
        principal = um.getPrincipal("user1");
        assertTrue(principal.isMemberOf(group.getName()));

        // commit directory changes
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // When i POST this group
        httpClient.buildDeleteRequest("/user/" + principal.getName() + "/group/" + group.getName())
                  .executeAndConsume(new HttpStatusCodeHandler(), status -> assertEquals(SC_OK, status.intValue()));

        principal = um.getPrincipal(principal.getName());
        assertFalse(principal.isMemberOf(group.getName()));
    }

    @Test
    public void itCanSearchUsers() {
        // Given a search string
        httpClient.buildGetRequest("/user/search")
                  .addQueryParameter("q", "Steve")
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals("null", node.get("errorMessage").asText());
                      ArrayNode entries = (ArrayNode) node.get("entries");
                      assertEquals(1, entries.size());
                      assertEquals("user0", entries.get(0).get("id").asText());
                  });
    }

    @Test
    public void itCanPaginateUsers() {

        String[][] expectedPages = new String[][] { new String[] { "Administrator", "foouser", "Guest" },
                new String[] { "user0", "user1", "user2" }, new String[] { "user3", "user4" } };

        for (int i = 0; i < expectedPages.length; i++) {
            int iFinal = i;
            httpClient.buildGetRequest("/user/search")
                      .addQueryParameters(getQueryParamsForPage(i))
                      .executeAndConsume(new JsonNodeHandler(), node -> {
                          assertPaging(iFinal, 3, 3, 8, expectedPages[iFinal].length, node);
                          assertUserEntries(node, expectedPages[iFinal]);
                      });
        }
    }

    @Test
    public void itCanSearchGroups() {
        // Given a search string
        httpClient.buildGetRequest("/group/search")
                  .addQueryParameter("q", "Lannister")
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals("null", node.get("errorMessage").asText());
                      ArrayNode entries = (ArrayNode) node.get("entries");
                      assertEquals(1, entries.size());
                      assertEquals("Lannister", entries.get(0).get("grouplabel").asText());
                  });
    }

    @Test
    public void itCanPaginateGroups() {

        String[][] expectedResults = new String[][] { new String[] { "administrators", "foogroup", "group0" },
                new String[] { "group1", "group2", "group3" }, new String[] { "members", "powerusers" },
                new String[0], };

        for (int i = 0; i < expectedResults.length; i++) {
            int iFinal = i;
            httpClient.buildGetRequest("/group/search")
                      .addQueryParameters(getQueryParamsForPage(i))
                      .executeAndConsume(new JsonNodeHandler(), node -> {
                          assertPaging(iFinal, 3, 3, 8, expectedResults[iFinal].length, node);
                          assertGroupEntries(node, expectedResults[iFinal]);
                      });
        }

    }

    /**
     * @since 8.2
     */
    @Test
    public void itCanPaginateGroupMembers() {

        String[][] expectedResults = new String[][] { new String[] { "dummy", "dummy", "dummy" },
                new String[] { "dummy", "foouser" } };

        for (int i = 0; i < expectedResults.length; i++) {
            int iFinal = i;
            httpClient.buildGetRequest("/group/group1/@users")
                      .addQueryParameters(getQueryParamsForPage(i))
                      .executeAndConsume(new JsonNodeHandler(),
                              node -> assertPaging(iFinal, 3, 2, 5, expectedResults[iFinal].length, node));
        }
    }

    @Test
    public void itDoesntWritePassword() {
        // When I call JSON for user1
        httpClient.buildGetRequest("/user/user1")
                  .executeAndConsume(new JsonNodeHandler(),
                          // Then it doesn't contain the password at all
                          node -> assertNull("", node.get("properties").get("password")));
    }

    @Test
    public void itCanFetchATransientUser() {
        httpClient.buildGetRequest("/user/transient/foo@bar.com/666")
                  .executeAndConsume(new JsonNodeHandler(), node -> assertEqualsUser("transient/foo@bar.com/666",
                          "foo@bar.com", "null", "foo@bar.com", node));
    }

    /**
     * @since 10.10
     */
    @Test
    public void itCantCreateUserWithUnknownGroup() {

        NuxeoPrincipal principal = new NuxeoPrincipalImpl("newuser");
        principal.setFirstName("test");
        principal.setLastName("user");
        principal.setCompany("nuxeo");
        principal.setEmail("test@nuxeo.com");
        principal.setGroups(List.of("unknownGroup"));

        httpClient.buildPostRequest("/user")
                  .entity(getPrincipalAsJson(principal, "testPassword"))
                  .executeAndConsume(new JsonNodeHandler(SC_FORBIDDEN),
                          node -> assertEquals("group does not exist: unknownGroup", node.get("message").textValue()));
    }

    /**
     * @since 10.10
     */
    @Test
    public void itCantUpdateAUserWithUnknownGroup() throws IOException {

        NuxeoPrincipal user = um.getPrincipal("user1");
        user.setFirstName("Paul");
        user.setLastName("McCartney");
        user.setGroups(List.of("unknownGroup"));

        // When I call a PUT on the Rest endpoint
        httpClient.buildPutRequest("/user/user1")
                  .entity(getPrincipalAsJson(user))
                  .executeAndConsume(new JsonNodeHandler(SC_FORBIDDEN),
                          node -> assertEquals("group does not exist: unknownGroup", node.get("message").textValue()));
    }

    // NXP-22771
    @Test
    public void itReturnsAdminStatusAndGroupsForAdminOnly() {
        // When I GET the administrator user
        nonAdminHttpClient.buildGetRequest("/user/Administrator")
                          .executeAndConsume(new JsonNodeHandler(),
                                  // Then it does not tell the user is administrator
                                  // And it does not show its groups
                                  node -> {
                                      assertEqualsUser("Administrator", "", "", node);
                                      assertNull(node.get("isAdministrator"));
                                      assertNotNull(node.get("properties"));
                                      assertNull(node.get("properties").get("groups"));
                                  });
        // When I GET the user2
        nonAdminHttpClient.buildGetRequest("/user/user2")
                          .executeAndConsume(new JsonNodeHandler(),
                                  // Then it does not tell the user is not administrator
                                  // But it does show its groups
                                  node -> {
                                      assertEqualsUser("user2", "Georges", "Harrisson", node);
                                      assertNull(node.get("isAdministrator"));
                                      assertNotNull(node.get("properties"));
                                      assertNotNull(node.get("properties").get("groups"));
                                  });
    }

    // NXP-33128
    @Test
    @WithFrameworkProperty(name = GroupRootObject.RESTRICT_ADMINISTRATORS_MEMBERS_PROP, value = "true")
    public void itOnlyAdminCanReadAdminGroupMembers() {
        var parentGroup = new NuxeoGroupImpl("parentGroup");
        um.createGroup(parentGroup.getModel());

        // Given a non administrator group: group1
        var group = um.getGroup("group1");
        group.setMemberGroups(List.of("group2"));
        group.setParentGroups(List.of("parentGroup"));
        um.updateGroup(group.getModel());
        nextTransaction();

        // When I GET this group as a non administrator user
        nonAdminHttpClient.buildGetRequest("/group/" + group.getName())
                          .addQueryParameter(FETCH_PROPERTIES + "." + NuxeoGroupJsonWriter.ENTITY_TYPE,
                                  "memberUsers,memberGroups,parentGroups")
                          .executeAndConsume(new JsonNodeHandler(),
                                  // Then it returns the group's member users, member groups and parent groups
                                  node -> assertGroupMembers(node, false));

        // Given an administrator group: administrators
        group = um.getGroup("administrators");
        group.setMemberGroups(List.of("group3"));
        group.setParentGroups(List.of("parentGroup"));
        um.updateGroup(group.getModel());
        nextTransaction();

        // When I GET this group as a non administrator user
        nonAdminHttpClient.buildGetRequest("/group/" + group.getName())
                          .addQueryParameter(FETCH_PROPERTIES + "." + NuxeoGroupJsonWriter.ENTITY_TYPE,
                                  "memberUsers,memberGroups,parentGroups")
                          .executeAndConsume(new JsonNodeHandler(),
                                  // Then it doesn't return the group's member users, member groups and parent groups
                                  node -> assertGroupMembers(node, true));

        // When I GET this group as an administrator user
        httpClient.buildGetRequest("/group/" + group.getName())
                  .addQueryParameter(FETCH_PROPERTIES + "." + NuxeoGroupJsonWriter.ENTITY_TYPE,
                          "memberUsers,memberGroups,parentGroups")
                  .executeAndConsume(new JsonNodeHandler(),
                          // Then it returns the group's member users, member groups and parent groups
                          node -> assertGroupMembers(node, false));

        // Given a subgroup of an administrator group: group3
        group = um.getGroup("group3");
        group.setMemberUsers(List.of("user3", "user4"));
        group.setMemberGroups(List.of("subgroup"));
        um.updateGroup(group.getModel());
        nextTransaction();

        // When I GET this group as a non administrator user
        nonAdminHttpClient.buildGetRequest("/group/" + group.getName())
                          .addQueryParameter(FETCH_PROPERTIES + "." + NuxeoGroupJsonWriter.ENTITY_TYPE,
                                  "memberUsers,memberGroups,parentGroups")
                          .executeAndConsume(new JsonNodeHandler(),
                                  // Then it doesn't return the group's member users, member groups and parent groups
                                  node -> assertGroupMembers(node, true));

        // When I GET this group as an administrator user
        httpClient.buildGetRequest("/group/" + group.getName())
                  .addQueryParameter(FETCH_PROPERTIES + "." + NuxeoGroupJsonWriter.ENTITY_TYPE,
                          "memberUsers,memberGroups,parentGroups")
                  .executeAndConsume(new JsonNodeHandler(),
                          // Then it returns the group's member users, member groups and parent groups
                          node -> assertGroupMembers(node, false));
    }

    protected void assertGroupMembers(JsonNode groupNode, boolean empty) {
        JsonNode memberUsers = groupNode.get("memberUsers");
        assertNotNull(memberUsers);
        assertTrue(memberUsers.isArray());

        JsonNode memberGroups = groupNode.get("memberGroups");
        assertNotNull(memberGroups);
        assertTrue(memberGroups.isArray());

        JsonNode parentGroups = groupNode.get("parentGroups");
        assertNotNull(parentGroups);
        assertTrue(parentGroups.isArray());

        if (empty) {
            assertTrue(memberUsers.isEmpty());
            assertTrue(memberGroups.isEmpty());
            assertTrue(parentGroups.isEmpty());
        } else {
            assertFalse(memberUsers.isEmpty());
            assertFalse(memberGroups.isEmpty());
            assertFalse(parentGroups.isEmpty());
        }
    }

    /**
     * @param node node to test
     * @param users an array of expected user names
     * @since 5.8
     */
    private void assertUserEntries(JsonNode node, String... users) {
        ArrayNode entries = (ArrayNode) node.get("entries");
        assertEquals(users.length, entries.size());
        for (int i = 0; i < users.length; i++) {
            assertEquals(users[i], entries.get(i).get("id").asText());
        }
    }

    /**
     * @param currentPageIndex expected currentPage index
     * @param pageSize expected page size
     * @param numberOfPage expected number of page
     * @param resultsCount expected resultsCount
     * @param currentPageSize expected currentPageSize
     * @since 5.8
     */
    private void assertPaging(int currentPageIndex, int pageSize, int numberOfPage, int resultsCount,
            int currentPageSize, JsonNode node) {
        assertTrue(node.get("isPaginable").booleanValue());
        assertEquals(currentPageIndex, node.get("currentPageIndex").intValue());
        assertEquals(pageSize, node.get("pageSize").intValue());
        assertEquals(numberOfPage, node.get("numberOfPages").intValue());
        assertEquals(resultsCount, node.get("resultsCount").intValue());
        assertEquals(currentPageSize, node.get("currentPageSize").intValue());
    }

    /**
     * @since 5.8
     */
    private Map<String, String> getQueryParamsForPage(int pageIndex) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("q", "*");
        queryParams.put("currentPageIndex", Integer.toString(pageIndex));
        queryParams.put("pageSize", "3");
        return queryParams;
    }

    /**
     * @param node node to test
     * @param groups an array of expected group names
     * @since 5.8
     */
    private void assertGroupEntries(JsonNode node, String... groups) {
        ArrayNode entries = (ArrayNode) node.get("entries");
        assertEquals(groups.length, entries.size());
        for (int i = 0; i < groups.length; i++) {
            assertEquals(groups[i], entries.get(i).get("id").asText());
        }
    }

}
