/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
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
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.services.config.ConfigurationService;
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

    @Mock
    @RuntimeService
    protected AdministratorGroupsProvider administratorGroupsProvider;

    protected DocumentModel src;

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Mock
    @RuntimeService
    private UserManager userManager;

    private final GregorianCalendar begin = new GregorianCalendar(2015, Calendar.JUNE, 20, 12, 34, 56);

    private final GregorianCalendar end = new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56);

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
    public void canAddPermissionForExistingUser() throws OperationException {
        canAddPermissionFor("existingUser");
    }

    @Test
    public void canAddPermissionForExistingGroup() throws OperationException {
        canAddPermissionFor("existingGroup");
    }

    @Test
    public void cannotAddPermissionForNonExistentUser() throws OperationException {
        cannotAddPermissionFor("nonExistentUser");
    }

    @Test
    public void cannotAddPermissionForNonExistentGroup() throws OperationException {
        cannotAddPermissionFor("nonExistentGroup");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-allow-virtual-user.xml")
    public void canAddPermissionForNonExistentUser() throws OperationException {
        ConfigurationService configService = Framework.getService(ConfigurationService.class);
        assertTrue(configService.isBooleanPropertyTrue(AddPermission.ALLOW_VIRTUAL_USER));
        canAddPermissionFor("nonExistentUser");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-allow-virtual-user.xml")
    public void canAddPermissionForNonExistentGroup() throws OperationException {
        ConfigurationService configService = Framework.getService(ConfigurationService.class);
        assertTrue(configService.isBooleanPropertyTrue(AddPermission.ALLOW_VIRTUAL_USER));
        canAddPermissionFor("nonExistentGroup");
    }

    private void canAddPermissionFor(String existingGroupOrUser) throws OperationException {
        try {
            OperationContext ctx = new OperationContext(session);
            ctx.setInput(src);
            Map<String, Object> params = new HashMap<>();
            params.put("user", existingGroupOrUser);
            params.put("permission", "Write");
            params.put("begin", begin);
            params.put("end", end);

            assertNull(src.getACP().getACL(ACL.LOCAL_ACL));
            automationService.run(ctx, AddPermission.ID, params);
            assertNotNull(src.getACP().getACL(ACL.LOCAL_ACL));
            assertEquals(end, src.getACP().getACL(ACL.LOCAL_ACL).get(0).getEnd());
        } finally {
            // Tear down
            src.getACP().removeACEsByUsername(ACL.LOCAL_ACL, existingGroupOrUser);
        }
    }

    private void cannotAddPermissionFor(String nonExistentGroupOrUser) throws OperationException {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        Map<String, Object> params = new HashMap<>();
        params.put("user", nonExistentGroupOrUser);
        params.put("permission", "Write");
        params.put("begin", begin);
        params.put("end", end);

        assertNull(src.getACP().getACL(ACL.LOCAL_ACL));
        try {
            automationService.run(ctx, AddPermission.ID, params);
            fail();
        } catch (IllegalParameterException e) {
            String expectedMsg = String.format(
                    "Failed to invoke operation Document.AddPermission with aliases [Document.AddACL], "
                            + "User or group name '%s' does not exist. Please provide a valid name.",
                    nonExistentGroupOrUser);
            assertEquals(e.getMessage(), expectedMsg, e.getMessage());
        }
        assertNull(src.getACP().getACL(ACL.LOCAL_ACL));
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
