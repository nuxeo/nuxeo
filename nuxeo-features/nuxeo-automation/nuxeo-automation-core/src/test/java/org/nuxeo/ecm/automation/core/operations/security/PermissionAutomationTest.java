/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.security;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.AddPermission;
import org.nuxeo.ecm.automation.core.operations.document.BlockPermissionInheritance;
import org.nuxeo.ecm.automation.core.operations.document.ReplacePermission;
import org.nuxeo.ecm.automation.core.operations.document.UnblockPermissionInheritance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.AdministratorGroupsProvider;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, MockitoFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:test-operations.xml")
public class PermissionAutomationTest {

    protected DocumentModel src;

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Mock
    @RuntimeService
    protected AdministratorGroupsProvider administratorGroupsProvider;

    @Mock
    @RuntimeService
    protected UserManager userManager;

    @Before
    public void initRepo() throws Exception {
        src = session.createDocumentModel("/", "src", "Folder");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        when(userManager.getUserModel("existingUser")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getGroupModel("existingGroup")).thenReturn(new SimpleDocumentModel("group"));
        when(administratorGroupsProvider.getAdministratorsGroups()).thenReturn(
                Collections.singletonList("administrators"));
    }

    @Test
    public void canReplacePermission() throws OperationException {
        try {
            OperationContext ctx = new OperationContext(session);
            ctx.setInput(src);

            // Add permission
            Map<String, Object> params = new HashMap<>();
            params.put("user", "existingUser");
            params.put("permission", "Write");
            automationService.run(ctx, AddPermission.ID, params);
            ctx.setInput(src);

            // Replace permission
            params.put("user", "existingUser");
            params.put("permission", "Everything");
            params.put("id", "existingUser:Write:true:Administrator::");

            assertEquals("Write", src.getACP().getACL(ACL.LOCAL_ACL).get(0).getPermission());
            automationService.run(ctx, ReplacePermission.ID, params);
            src.refresh();
            assertEquals("Everything", src.getACP().getACL(ACL.LOCAL_ACL).get(0).getPermission());
        } finally {
            // Tear down
            src.getACP().removeACEsByUsername(ACL.LOCAL_ACL, "existingUser");
        }
    }

    @Test
    public void canBlockPermissionInheritance() throws OperationException {
        ACP acp = src.getACP();
        assertEquals(Access.GRANT, acp.getAccess("members", READ));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        automationService.run(ctx, BlockPermissionInheritance.ID);
        src.refresh();
        acp = src.getACP();
        assertEquals(Access.DENY, acp.getAccess("members", READ));
    }

    @Test
    public void canUnblockPermissionInheritance() throws OperationException {
        ACP acp = src.getACP();
        acp.blockInheritance(ACL.LOCAL_ACL, session.getPrincipal().getName());
        assertEquals(Access.DENY, acp.getAccess("members", READ));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        automationService.run(ctx, UnblockPermissionInheritance.ID);
        src.refresh();
        acp = src.getACP();
        assertEquals(Access.GRANT, acp.getAccess("members", READ));
    }
}
