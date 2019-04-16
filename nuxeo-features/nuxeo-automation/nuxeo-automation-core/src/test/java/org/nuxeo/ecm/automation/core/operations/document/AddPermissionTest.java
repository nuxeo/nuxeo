/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuno Cunha (ncunha@nuxeo.com)
 */

package org.nuxeo.ecm.automation.core.operations.document;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.AdministratorGroupsProvider;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, MockitoFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:test-operations.xml")
public class AddPermissionTest {

    protected final String USER = "user";

    protected final String GROUP = "group";

    protected final String UNKNOWN = "unknown";

    @Inject
    protected AutomationService automationService;

    @Inject
    protected CoreSession session;

    @Mock
    @RuntimeService
    protected AdministratorGroupsProvider administratorGroupsProvider;

    @Mock
    @RuntimeService
    protected UserManager userManager;

    @Before
    public void initRepo() {
        DocumentModel doc = session.createDocumentModel("/", "src", "File");
        session.createDocument(doc);
        session.save();
    }

    @After
    public void clearPermissions() {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        doc.getACP().removeACL("local");
        session.saveDocument(doc);
    }

    @Test
    public void shouldFailWhenNoUsersOrEmailAreProvided() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, null, null, "Write", null, null, null, false,
                false, null);

        try {
            automationService.run(ctx, AddPermission.ID, params);
            fail();
        } catch (IllegalParameterException e) {
            String expectedMsg = "'users' or 'email' parameters must be set";
            assertEquals(expectedMsg, e.getOriginalMessage());
            verifyZeroInteractions(userManager);
        }
    }

    @Test
    public void shouldFailWhenEmailIsProvidedButNotEndDate() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, null, "johndoe@nuxeo.com", "Write", null, null,
                null, false, false, null);

        try {
            automationService.run(ctx, AddPermission.ID, params);
            fail();
        } catch (IllegalParameterException e) {
            String expectedMsg = "'end' parameter must be set when adding a permission for an 'email'";
            assertEquals(expectedMsg, e.getOriginalMessage());
            verifyZeroInteractions(userManager);
        }
    }

    @Test
    public void shouldFailWhenSomeUserDoesNotExist() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "src", "File");
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        List<String> users = Arrays.asList("existingGroup", "unexistingUser", "existingUser", "unexistingUser2");
        when(userManager.getUserModel("existingUser")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getGroupModel("existingGroup")).thenReturn(new SimpleDocumentModel("group"));
        when(userManager.getUserModel("unexistingUser")).thenReturn(null);
        when(userManager.getUserModel("unexistingUser2")).thenReturn(null);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, users, null, "Write", null, null, null, false,
                false, null);

        try {
            automationService.run(ctx, AddPermission.ID, params);
            fail();
        } catch (IllegalParameterException e) {
            String expectedMsg = "The following set of User or Group names do not exist: [unexistingUser,unexistingUser2]. Please provide valid ones.";
            assertEquals(expectedMsg, e.getOriginalMessage());

            verifyUserOrGroup("existingGroup", GROUP);
            verifyUserOrGroup("unexistingUser", UNKNOWN);
            verifyUserOrGroup("existingUser", USER);
            verifyUserOrGroup("unexistingUser2", UNKNOWN);
            verifyNoMoreInteractions(userManager);
        }
    }

    @Test
    public void shouldFailWhenSomeGroupDoesNotExist() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "src", "File");
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        List<String> users = Arrays.asList("existingGroup", "unexistingGroup", "existingUser");
        when(userManager.getUserModel("existingUser")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getGroupModel("existingGroup")).thenReturn(new SimpleDocumentModel("group"));
        when(userManager.getGroupModel("unexistingGroup")).thenReturn(null);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, users, null, "Write", null, null, null, false,
                false, null);

        try {
            automationService.run(ctx, AddPermission.ID, params);
            fail();
        } catch (IllegalParameterException e) {
            String expectedMsg = "The following set of User or Group names do not exist: [unexistingGroup]. Please provide valid ones.";
            assertEquals(expectedMsg, e.getOriginalMessage());

            verifyUserOrGroup("existingGroup", GROUP);
            verifyUserOrGroup("unexistingGroup", UNKNOWN);
            verifyUserOrGroup("existingUser", USER);
            verifyNoMoreInteractions(userManager);
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-allow-virtual-user.xml")
    public void shouldFailWhenPermissionDoesNotExist() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "src", "File");
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        List<String> users = Arrays.asList("foo");
        String invalidPermission = "<a href=www.evil.com>New";
        Map<String, Object> params = getParametersForAddOperation(null, users, null, invalidPermission, null, null,
                null, false, false, null);
        try {
            automationService.run(ctx, AddPermission.ID, params);
            fail(String.format("Calling %s with an invalid permission %s should fail.", AddPermission.ID,
                    invalidPermission));
        } catch (IllegalParameterException e) {
            assertEquals(String.format("Permission %s is invalid.", invalidPermission), e.getOriginalMessage());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-allow-virtual-user.xml")
    public void shouldAddPermissionForUnexistingUserWhenAllowVirtualUserFlagIsTrue() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        when(userManager.getUserModel("unexistingUser")).thenReturn(null);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, singletonList("unexistingUser"), null, "Write",
                "", null, null, false, false, null);

        automationService.run(ctx, AddPermission.ID, params);
        verifyZeroInteractions(userManager);

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(1, acl.size());

        assertExpectedPermissions(acl, params, "unexistingUser");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-allow-virtual-user.xml")
    public void shouldAddPermissionForUnexistingGroupsWhenAllowVirtualUserFlagIsTrue() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        List<String> groups = Arrays.asList("unexistingGroup1", "unexistingGroup2");
        groups.forEach(group -> when(userManager.getGroupModel(group)).thenReturn(null));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, groups, null, "Read", null,
                new GregorianCalendar(2018, 8, 2), new GregorianCalendar(2018, 8, 8), false, true, "Permission Given");

        automationService.run(ctx, AddPermission.ID, params);
        verifyZeroInteractions(userManager);

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(2, acl.size());

        assertExpectedPermissions(acl, params, "unexistingGroup1");
        assertExpectedPermissions(acl, params, "unexistingGroup2");
    }

    @Test
    public void shouldAddPermissionWhenUsingDeprecatedParameterUser() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        when(userManager.getUserModel("existingUser")).thenReturn(new SimpleDocumentModel("user"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation("existingUser", null, null, "Read", null,
                new GregorianCalendar(2018, 8, 2), new GregorianCalendar(2018, 8, 8), false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        verifyUserOrGroup("existingUser", USER);
        verifyNoMoreInteractions(userManager);

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(1, acl.size());

        assertExpectedPermissions(acl, params, "existingUser");
    }

    @Test
    public void shouldAddPermissionWhenUsingEmailAndEndDateParameters() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, null, "jane@nuxeo.com", "Write", null, null,
                new GregorianCalendar(2018, 8, 8), false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        verifyZeroInteractions(userManager);

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(1, acl.size());

        assertExpectedPermissions(acl, params, "jane@nuxeo.com");
    }

    @Test
    public void shouldAddPermissionWhenUsingDeprecatedUserAndUsersParametersCombined() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        List<String> users = Arrays.asList("existingUser2", "existingUser3");
        when(userManager.getUserModel("existingUser1")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getUserModel("existingUser2")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getUserModel("existingUser3")).thenReturn(new SimpleDocumentModel("user"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation("existingUser1", users, null, "Write", null, null,
                null, false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        verifyUserOrGroup("existingUser1", USER);
        verifyUserOrGroup("existingUser2", USER);
        verifyUserOrGroup("existingUser3", USER);
        verifyNoMoreInteractions(userManager);

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(3, acl.size());

        assertExpectedPermissions(acl, params, "existingUser1");
        assertExpectedPermissions(acl, params, "existingUser2");
        assertExpectedPermissions(acl, params, "existingUser3");
    }

    @Test
    public void shouldAddPermissionForUsersOnlyWhenUsingEmailAndUsersParametersCombined() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        List<String> users = Arrays.asList("existingUser1", "existingUser2");
        when(userManager.getUserModel("existingUser1")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getUserModel("existingUser2")).thenReturn(new SimpleDocumentModel("user"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, users, "user@nuxeo.com", "Write", null, null,
                new GregorianCalendar(2018, 8, 8), false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        verifyUserOrGroup("existingUser1", USER);
        verifyUserOrGroup("existingUser2", USER);
        verifyNoMoreInteractions(userManager);

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(2, acl.size());

        assertExpectedPermissions(acl, params, "existingUser1");
        assertExpectedPermissions(acl, params, "existingUser2");
    }

    @Test
    public void shouldAddPermissionForUserOnlyWhenUsingEmailAndUserParametersCombined() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        when(userManager.getUserModel("existingUser1")).thenReturn(new SimpleDocumentModel("user"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation("existingUser1", null, "user@nuxeo.com", "Write",
                null, null, new GregorianCalendar(2018, 8, 8), false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        verifyUserOrGroup("existingUser1", USER);
        verifyNoMoreInteractions(userManager);

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(1, acl.size());

        assertExpectedPermissions(acl, params, "existingUser1");
    }

    @Test
    public void shouldAddPermissionForUserWhenBlockingInheritance() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        when(administratorGroupsProvider.getAdministratorsGroups()).thenReturn(singletonList("administrators"));

        when(userManager.getUserModel("existingUser1")).thenReturn(new SimpleDocumentModel("user"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, singletonList("existingUser1"), null, "Write",
                null, null, new GregorianCalendar(2018, 8, 8), true, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        verifyUserOrGroup("existingUser1", USER);
        verifyNoMoreInteractions(userManager);

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertThat(acl.size(), greaterThan(1));

        assertExpectedPermissions(acl, params, "existingUser1");
    }

    protected void assertExpectedPermissions(ACL acl, Map<String, Object> params, String userOrGroup) {
        ACE ace = acl.stream().filter(el -> {
            String email = (String) params.get("email");
            if (email != null && email.equals(userOrGroup)) {
                return el.getUsername().contains(email);
            }
            return el.getUsername().equals(userOrGroup);
        }).findFirst().orElse(null);
        assertNotNull(ace);
        assertEquals(params.get("permission"), ace.getPermission());
        assertEquals(params.get("begin"), ace.getBegin());
        assertEquals(params.get("end"), ace.getEnd());
        // None of the others are stored on the database, neither the context data
    }

    protected void verifyUserOrGroup(String userOrGroup, String expectedToBeFindAs) {
        switch (expectedToBeFindAs) {
        case USER:
            verify(userManager).getUserModel(userOrGroup);
            break;
        default:
            verify(userManager).getUserModel(userOrGroup);
            verify(userManager).getGroupModel(userOrGroup);
        }
    }

    protected Map<String, Object> getParametersForAddOperation(String user, List<String> users, String email,
            String permission, String aclName, Calendar begin, Calendar end, boolean blockInheritance, boolean notify,
            String comment) {
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("users", users);
        params.put("email", email);
        params.put("permission", permission);
        params.put("aclName", aclName);
        params.put("begin", begin);
        params.put("end", end);
        params.put("blockInheritance", blockInheritance);
        params.put("notify", notify);
        params.put("comment", comment);
        return params;
    }
}
