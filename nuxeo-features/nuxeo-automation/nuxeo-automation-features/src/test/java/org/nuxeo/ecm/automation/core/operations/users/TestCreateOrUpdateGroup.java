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
import static org.junit.Assert.assertNotNull;
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoGroup;
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
public class TestCreateOrUpdateGroup {

    public static final String GROUP = "group";

    public static final String GROUP2 = "group2";

    public static final String GROUP3 = "group3";

    public static final String GROUPLABEL = "grouplabel";

    public static final String GROUPLABEL2 = "grouplabel2";

    public static final String USER1 = "bob";

    public static final String USER2 = "pete";

    public static final String USER3 = "steve";

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
            params.put("groupname", GROUP);
            params.put("grouplabel", GROUPLABEL);
            params.put("members", new String[] { USER1 });
            params.put("parentGroups", new String[] { GROUP2 });

            automationService.run(ctx, CreateOrUpdateGroup.ID, params);

            NuxeoGroup group = userManager.getGroup(GROUP);
            assertNotNull(group);
            assertEquals(GROUPLABEL, group.getLabel());
            assertEquals(Arrays.asList(GROUP2), group.getParentGroups());
            assertEquals(Arrays.asList(USER1), group.getMemberUsers());

            // cannot create if mode = create and the group exists
            params.put("mode", "create");
            try {
                automationService.run(ctx, CreateOrUpdateGroup.ID, params);
            } catch (OperationException e) {
                String msg = e.getMessage();
                assertTrue(msg, msg.contains("Cannot create already-existing group: group"));
            }
        }
    }

    @Test
    public void testUpdate() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();
            params.put("groupname", GROUP);
            params.put("grouplabel", GROUPLABEL);
            params.put("members", new String[] { USER1 });
            params.put("parentGroups", new String[] { GROUP2 });

            // cannot update if mode = update and the group does not exists
            params.put("mode", "update");
            try {
                automationService.run(ctx, CreateOrUpdateGroup.ID, params);
            } catch (OperationException e) {
                String msg = e.getMessage();
                assertTrue(msg, msg.contains("Cannot update non-existent group: group"));
            }

            // create requested
            params.put("mode", "create");

            automationService.run(ctx, CreateOrUpdateGroup.ID, params);

            NuxeoGroup group = userManager.getGroup(GROUP);
            assertNotNull(group);
            assertEquals(GROUPLABEL, group.getLabel());
            assertEquals(Arrays.asList(GROUP2), group.getParentGroups());
            assertEquals(Arrays.asList(USER1), group.getMemberUsers());

            // now update
            params = new HashMap<>();
            params.put("mode", "update");
            params.put("groupname", GROUP);
            params.put("grouplabel", GROUPLABEL2);
            params.put("members", new String[] { USER2, USER3 });
            params.put("parentGroups", new String[] { GROUP3 });

            automationService.run(ctx, CreateOrUpdateGroup.ID, params);

            group = userManager.getGroup(GROUP);
            assertNotNull(group);
            assertEquals(GROUPLABEL2, group.getLabel());
            // list of parent groups is replaced
            assertEquals(Arrays.asList(GROUP3), group.getParentGroups());
            assertEquals(Arrays.asList(USER2, USER3), group.getMemberUsers());

            // clear parent groups
            params = new HashMap<>();
            params.put("mode", "update");
            params.put("groupname", GROUP);
            params.put("parentGroups", new String[0]);

            automationService.run(ctx, CreateOrUpdateGroup.ID, params);

            group = userManager.getGroup(GROUP);
            assertNotNull(group);
            // list of parent groups is cleared
            assertEquals(Collections.emptyList(), group.getParentGroups());

            // update sub groups
            params = new HashMap<>();
            params.put("mode", "update");
            params.put("groupname", GROUP);
            params.put("subGroups", new String[] { GROUP2 });

            automationService.run(ctx, CreateOrUpdateGroup.ID, params);

            group = userManager.getGroup(GROUP);
            assertNotNull(group);
            // list of parent groups is cleared
            assertEquals(Arrays.asList(GROUP2), group.getMemberGroups());
        }
    }

}
