/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.users;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link SuggestUserEntries} operation.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.automation.features:test-user-directories-contrib.xml")
public class TestSuggestUserEntries {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected UserManager userManager;

    @Test
    public void testUserEntries() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {

            // check operation result JSON properties
            Map<String, String> params = new HashMap<>();
            params.put("searchType", "USER_TYPE");
            params.put("searchTerm", "adm");
            Blob result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);

            JsonAssert json = JsonAssert.on(result.getString()).isArray().length(1);
            JsonAssert entry = json.get(0);
            entry.has("type").isEquals("USER_TYPE");
            entry.has("id").isEquals("Administrator");
            entry.has("prefixed_id").isEquals("user:Administrator");
            entry.has("username").isEquals("Administrator");
            entry.has("firstName").isEquals("Jacky");
            entry.has("lastName").isEquals("Chan");
            entry.has("company").isEquals("Nuxeo");
            entry.has("email").isEquals("devnull@nuxeo.com");
            entry.has("tenantId").isNull();
            entry.has("displayIcon").isTrue();
            entry.has("displayLabel").isEquals("Jacky Chan");
            entry.has("groups").isArray().length(0);

            // check various suggestion cases
            // null search term, expecting all users
            params.remove("searchTerm");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(3);
            json.childrenContains("id", "Administrator", "jdoe", "jack");

            // empty search term, expecting all users
            params.put("searchTerm", "");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(3);
            json.childrenContains("id", "Administrator", "jdoe", "jack");

            // term not matching any user
            params.put("searchTerm", "foo");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(0);

            // term matching several users
            params.put("searchTerm", "j");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(3);
            json.childrenContains("id", "Administrator", "jdoe", "jack"); // Yes, Administrator's first name is Jacky...

            // term matching a user's first name
            params.put("searchTerm", "Joh");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(1);
            json.childrenContains("id", "jdoe");

            // term matching a user's lastname
            params.put("searchTerm", "Do");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(1);
            json.childrenContains("id", "jdoe");

            params.put("searchTerm", "from New");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(1);
            json.childrenContains("id", "jack");

            // term matching a user's full name
            params.put("searchTerm", "John Do");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(1);
            json.childrenContains("id", "jdoe");

            params.put("searchTerm", "Jack fro");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(1);
            json.childrenContains("id", "jack");

            params.put("searchTerm", "Jack from New Jersey");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(1);
            json.childrenContains("id", "jack");

            // check the search term is trimmed
            params.put("searchTerm", "  John  Do  ");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(1);
            json.childrenContains("id", "jdoe");
        }
    }

    /*
     * Test that group restriction on the first entries checked doesn't prevent returning further ones.
     */
    @Test
    public void testUserEntriesWithGroupRestriction() throws Exception {
        // create group
        DocumentModel group = userManager.getBareGroupModel();
        group.setPropertyValue("group:groupname", "devs");
        userManager.createGroup(group);

        // create users
        int n = 20;
        for (int i = 0; i < n; i++) {
            DocumentModel user = userManager.getBareUserModel();
            user.setPropertyValue("user:username", "user" + i);
            user.setPropertyValue("user:firstName", "User" + i);
            user.setPropertyValue("user:lastName", "Smith");
            user.setPropertyValue("user:email", "user" + i + "@example.com");
            if ((i % 2) == 0) { // one in two belongs to devs
                user.setPropertyValue("user:groups", (Serializable) Arrays.asList("devs"));
            }
            userManager.createUser(user);
        }

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, String> params = new HashMap<>();
            params.put("searchType", "USER_TYPE");
            params.put("searchTerm", "Smith");
            params.put("groupRestriction", "devs");
            // using a max number of results makes the underlying search use a limit -- which is the original problem
            params.put("userSuggestionMaxSearchResults", "10");
            Blob result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);
            JsonAssert json = JsonAssert.on(result.getString());
            // despite the first internal search having had entries removed due to group restrictions,
            // we still get all we want
            assertEquals(10, json.getNode().size());
        }
    }

    @Test
    public void testGroupEntries() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {

            // check operation result JSON properties
            Map<String, String> params = new HashMap<>();
            params.put("searchType", "GROUP_TYPE");
            params.put("searchTerm", "adm");
            Blob result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);

            JsonAssert json = JsonAssert.on(result.getString()).isArray().length(1);
            JsonAssert entry = json.get(0);
            entry.has("type").isEquals("GROUP_TYPE");
            entry.has("id").isEquals("administrators");
            entry.has("prefixed_id").isEquals("group:administrators");
            entry.has("groupname").isEquals("administrators");
            entry.has("grouplabel").isEquals("Administrators group");
            entry.has("description").isEquals("Group of users with adminstrative rights");
            entry.has("tenantId").isNull();
            entry.has("displayIcon").isTrue();
            entry.has("displayLabel").isEquals("Administrators group");
            entry.has("members").isArray().length(0);
            entry.has("parentGroups").isArray().length(0);
            entry.has("subGroups").isArray().length(0);

            // check various suggestion cases
            // term not matching any group
            params.put("searchTerm", "foo");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(0);

            // term matching several groups
            params.put("searchTerm", "m");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(2);
            json.childrenContains("id", "members", "myTestGroup");

            // term matching a group's name
            params.put("searchTerm", "mem");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(1);
            json.childrenContains("id", "members");

            // term matching a group's label
            params.put("searchTerm", "group of");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).length(1);
            json.childrenContains("id", "members");
        }
    }

    @Test
    public void testMixedEntries() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {

            Map<String, String> params = new HashMap<>();
            params.put("searchTerm", "adm");
            Blob result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            assertNotNull(result);

            // term matching a user and a group
            JsonAssert json = JsonAssert.on(result.getString()).isArray().length(2);
            json.childrenContains("id", "Administrator", "administrators");

            // term matching a user only
            params.put("searchTerm", "Joh");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).isArray().length(1);
            json.childrenContains("id", "jdoe");

            // term matching a group only
            params.put("searchTerm", "mem");
            result = (Blob) automationService.run(ctx, SuggestUserEntries.ID, params);
            json = JsonAssert.on(result.getString()).isArray().length(1);
            json.childrenContains("id", "members");
        }
    }

}
