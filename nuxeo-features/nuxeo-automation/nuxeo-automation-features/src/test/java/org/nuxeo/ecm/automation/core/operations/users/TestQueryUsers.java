/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.core.operations.users;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.automation.features", //
        "org.nuxeo.ecm.platform.usermanager.api", //
        "org.nuxeo.ecm.platform.usermanager", //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.usermanager.tests:test-user-directories-contrib.xml", //
})
public class TestQueryUsers {

    public static final String USERNAME = "testuser";

    public static final String FIRSTNAME = "syd";

    public static final String LASTNAME = "barrett";

    public static final String USERNAME2 = "testuser2";

    public static final String LASTNAME2 = "vycyous";

    public static final String TENANTID = "alternatey";

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected AutomationService automationService;

    private static final Map<String, Object> map(Object... args) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < args.length;) {
            map.put((String) args[i++], args[i++]);
        }
        return map;
    }

    protected static void assertJsonUserList(List<Map<String, Object>> expected, Blob blob) throws Exception {
        String json = blob.getString();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> res = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        @SuppressWarnings("unchecked")
        List<Map<String, String>> users = (List<Map<String, String>>) res.get("users");
        users.sort((a, b) -> a.get("username").compareTo(b.get("username")));
        assertEquals(expected, users);
    }

    @Before
    public void setUp() {
        // first user
        DocumentModel userDoc = userManager.getBareUserModel();
        userDoc.setProperty("user", "username", USERNAME);
        userDoc.setProperty("user", "firstName", FIRSTNAME);
        userDoc.setProperty("user", "lastName", LASTNAME);
        userManager.createUser(userDoc);

        // other user with tenant
        userDoc = userManager.getBareUserModel();
        userDoc.setProperty("user", "username", USERNAME2);
        userDoc.setProperty("user", "firstName", FIRSTNAME);
        userDoc.setProperty("user", "lastName", LASTNAME2);
        userDoc.setProperty("user", "tenantId", TENANTID);
        userManager.createUser(userDoc);
    }

    @Test
    public void testQueryPattern() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            // search
            Map<String, Object> params = new HashMap<>();
            params.put("pattern", FIRSTNAME);
            Blob blob = (Blob) automationService.run(ctx, QueryUsers.ID, params);
            assertJsonUserList(Arrays.asList( //
                    map( //
                            "username", "testuser", //
                            "firstName", "syd", //
                            "lastName", "barrett"), //
                    map( //
                            "username", "testuser2", //
                            "firstName", "syd", //
                            "lastName", "vycyous", //
                            "tenantId", "alternatey" //
                    )), blob);

            // search filtering by tenant
            params = new HashMap<>();
            params.put("pattern", FIRSTNAME);
            params.put("tenantId", TENANTID);
            blob = (Blob) automationService.run(ctx, QueryUsers.ID, params);
            assertJsonUserList(Arrays.asList( //
                    map( //
                            "username", "testuser2", //
                            "firstName", "syd", //
                            "lastName", "vycyous", //
                            "tenantId", "alternatey" //
                    )), blob);
        }
    }

    @Test
    public void testQueryField() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            // search by first name
            Map<String, Object> params = new HashMap<>();
            params.put("firstName", FIRSTNAME);
            Blob blob = (Blob) automationService.run(ctx, QueryUsers.ID, params);
            assertJsonUserList(Arrays.asList( //
                    map( //
                            "username", "testuser", //
                            "firstName", "syd", //
                            "lastName", "barrett"), //
                    map( //
                            "username", "testuser2", //
                            "firstName", "syd", //
                            "lastName", "vycyous", //
                            "tenantId", "alternatey" //
                    )), blob);

            // search by first name AND last name
            params = new HashMap<>();
            params.put("firstName", FIRSTNAME);
            params.put("lastName", LASTNAME);
            blob = (Blob) automationService.run(ctx, QueryUsers.ID, params);
            assertJsonUserList(Arrays.asList( //
                    map( //
                            "username", "testuser", //
                            "firstName", "syd", //
                            "lastName", "barrett" //
                    )), blob);

            // search filtering by tenant
            params = new HashMap<>();
            params.put("firstName", FIRSTNAME);
            params.put("tenantId", TENANTID);
            blob = (Blob) automationService.run(ctx, QueryUsers.ID, params);
            assertJsonUserList(Arrays.asList( //
                    map( //
                            "username", "testuser2", //
                            "firstName", "syd", //
                            "lastName", "vycyous", //
                            "tenantId", "alternatey" //
                    )), blob);
        }
    }

}
