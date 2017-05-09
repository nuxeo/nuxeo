/*
 * (C) Copyright 2016-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Michael Vachette
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.core.operations.users;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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
public class TestCreateOrUpdateUser {

    public static final String USERNAME = "testuser";

    public static final String PASSWORD = "yo";

    public static final String FIRSTNAME = "mika";

    public static final String FIRSTNAME2 = "flo";

    public static final String GROUP2 = "group2";

    public static final String GROUP1 = "group1";

    public static final String MEMBERS = "members";

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected AutomationService automationService;

    @Test
    public void testCreate() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();
            params.put("username", USERNAME);
            params.put("password", PASSWORD);
            params.put("groups", new String[] { MEMBERS });
            Properties properties = new Properties();
            properties.put("firstName", FIRSTNAME);
            params.put("properties", properties);

            automationService.run(ctx, CreateOrUpdateUser.ID, params);

            NuxeoPrincipal principal = userManager.getPrincipal(USERNAME);
            assertEquals(FIRSTNAME, principal.getFirstName());
            assertEquals(Arrays.asList(MEMBERS), principal.getGroups());

            // cannot create if mode = create and the user exists
            params.put("mode", "create");
            try {
                automationService.run(ctx, CreateOrUpdateUser.ID, params);
            } catch (OperationException e) {
                String msg = e.getMessage();
                assertTrue(msg, msg.contains("Cannot create already-existing user: testuser"));
            }
        }
    }

    @Test
    public void testUpdate() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();
            params.put("username", USERNAME);
            params.put("password", PASSWORD);
            params.put("firstName", FIRSTNAME);
            params.put("groups", new String[] { MEMBERS });

            // cannot update if mode = update and the user does not exists
            params.put("mode", "update");
            try {
                automationService.run(ctx, CreateOrUpdateUser.ID, params);
            } catch (OperationException e) {
                String msg = e.getMessage();
                assertTrue(msg, msg.contains("Cannot update non-existent user: testuser"));
            }

            // create requested
            params.put("mode", "create");
            automationService.run(ctx, CreateOrUpdateUser.ID, params);

            NuxeoPrincipal principal = userManager.getPrincipal(USERNAME);
            assertEquals(FIRSTNAME, principal.getFirstName());
            assertEquals(Arrays.asList(MEMBERS), principal.getGroups());

            // now update, use properties
            params = new HashMap<>();
            params.put("mode", "update");
            params.put("username", USERNAME);
            params.put("groups", new String[] { GROUP1, GROUP2 });
            Properties properties = new Properties();
            properties.put("firstName", FIRSTNAME2);
            params.put("properties", properties);

            automationService.run(ctx, CreateOrUpdateUser.ID, params);

            principal = userManager.getPrincipal(USERNAME);
            assertEquals(FIRSTNAME2, principal.getFirstName());
            assertEquals(Arrays.asList(GROUP1, GROUP2), principal.getGroups());

            // clear groups
            params = new HashMap<>();
            params.put("mode", "update");
            params.put("username", USERNAME);
            params.put("groups", new String[0]);

            automationService.run(ctx, CreateOrUpdateUser.ID, params);

            principal = userManager.getPrincipal(USERNAME);
            assertEquals(Collections.emptyList(), principal.getGroups());
        }
    }

}
