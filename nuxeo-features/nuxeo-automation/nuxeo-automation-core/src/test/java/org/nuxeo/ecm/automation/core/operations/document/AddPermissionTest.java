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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @Inject
    protected AutomationService automationService;

    @Inject
    protected CoreSession session;

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
        }
    }

    @Test
    public void shouldFailWhenEmailIsProvidedButNotEndDate() throws OperationException {
        DocumentModel doc = session.getDocument(new PathRef("/src"));

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
        }
    }

    @Test
    public void shouldFailWhenSomeUserDoesNotExist() throws OperationException {
        when(userManager.getUserModel("existingUser")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getGroupModel("existingGroup")).thenReturn(new SimpleDocumentModel("group"));
        when(userManager.getUserModel("unexistingUser")).thenReturn(null);

        DocumentModel doc = session.createDocumentModel("/", "src", "File");

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null,
                Arrays.asList("existingGroup", "unexistingUser", "existingUser"), null, "Write", null, null, null,
                false, false, null);

        try {
            automationService.run(ctx, AddPermission.ID, params);
            fail();
        } catch (IllegalParameterException e) {
            String expectedMsg = String.format("User or group name '%s' does not exist. Please provide a valid name.",
                    "unexistingUser");
            assertEquals(expectedMsg, e.getOriginalMessage());
        }
    }

    @Test
    public void shouldFailWhenSomeGroupDoesNotExist() throws OperationException {
        when(userManager.getUserModel("existingUser")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getGroupModel("existingGroup")).thenReturn(new SimpleDocumentModel("group"));
        when(userManager.getGroupModel("unexistingGroup")).thenReturn(null);

        DocumentModel doc = session.createDocumentModel("/", "src", "File");

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null,
                Arrays.asList("existingGroup", "unexistingGroup", "existingUser"), null, "Write", null, null, null,
                false, false, null);

        try {
            automationService.run(ctx, AddPermission.ID, params);
            fail();
        } catch (IllegalParameterException e) {
            String expectedMsg = String.format("User or group name '%s' does not exist. Please provide a valid name.",
                    "unexistingGroup");
            assertEquals(expectedMsg, e.getOriginalMessage());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-allow-virtual-user.xml")
    public void shouldAddPermissionForUnexistingUserWhenAllowVirtualUserFlagIsTrue() throws OperationException {
        when(userManager.getUserModel("unexistingUser")).thenReturn(null);

        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, Collections.singletonList("unexistingUser"),
                null, "Write", "", null, null, false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        assertNotNull(doc.getACP());

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(1, acl.size());

        assertExpectedPermissions(acl, params, "unexistingUser");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.core:test-allow-virtual-user.xml")
    public void shouldAddPermissionForUnexistingGroupsWhenAllowVirtualUserFlagIsTrue() throws OperationException {
        List<String> groups = Arrays.asList("unexistingGroup1", "unexistingGroup2");
        groups.forEach(group -> when(userManager.getGroupModel(group)).thenReturn(null));

        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, groups, null, "Read", null,
                new GregorianCalendar(2018, 8, 2), new GregorianCalendar(2018, 8, 8), false, true, "Permission Given");

        automationService.run(ctx, AddPermission.ID, params);

        assertNotNull(doc.getACP());

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(2, acl.size());

        assertExpectedPermissions(acl, params, "unexistingGroup1");
        assertExpectedPermissions(acl, params, "unexistingGroup2");
    }

    @Test
    public void shouldAddPermissionWhenUsingDeprecatedParameterUser() throws OperationException {
        when(userManager.getUserModel("existingUser")).thenReturn(new SimpleDocumentModel("user"));

        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation("existingUser", null, null, "Read", null,
                new GregorianCalendar(2018, 8, 2), new GregorianCalendar(2018, 8, 8), false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        assertNotNull(doc.getACP());

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

        assertNotNull(doc.getACP());

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(1, acl.size());

        assertExpectedPermissions(acl, params, "jane@nuxeo.com");
    }

    @Test
    public void shouldAddPermissionWhenUsingDeprecatedUserAndUsersParametersCombined() throws OperationException {
        when(userManager.getUserModel("existingUser1")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getUserModel("existingUser2")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getUserModel("existingUser3")).thenReturn(new SimpleDocumentModel("user"));

        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation("existingUser1",
                Arrays.asList("existingUser2", "existingUser3"), null, "Write", null, null, null, false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        assertNotNull(doc.getACP());

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(3, acl.size());

        assertExpectedPermissions(acl, params, "existingUser1");
        assertExpectedPermissions(acl, params, "existingUser2");
        assertExpectedPermissions(acl, params, "existingUser3");
    }

    @Test
    public void shouldAddPermissionForUsersOnlyWhenUsingEmailAndUsersParametersCombined() throws OperationException {
        when(userManager.getUserModel("existingUser1")).thenReturn(new SimpleDocumentModel("user"));
        when(userManager.getUserModel("existingUser2")).thenReturn(new SimpleDocumentModel("user"));

        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation(null, Arrays.asList("existingUser1", "existingUser2"),
                "user@nuxeo.com", "Write", null, null, new GregorianCalendar(2018, 8, 8), false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        assertNotNull(doc.getACP());

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(2, acl.size());

        assertExpectedPermissions(acl, params, "existingUser1");
        assertExpectedPermissions(acl, params, "existingUser2");
    }

    @Test
    public void shouldAddPermissionForUserOnlyWhenUsingEmailAndUserParametersCombined() throws OperationException {
        when(userManager.getUserModel("existingUser1")).thenReturn(new SimpleDocumentModel("user"));

        DocumentModel doc = session.getDocument(new PathRef("/src"));
        assertNotNull(doc.getACP());
        assertNull(doc.getACP().getACL("local"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = getParametersForAddOperation("existingUser1", null, "user@nuxeo.com", "Write",
                null, null, new GregorianCalendar(2018, 8, 8), false, false, null);

        automationService.run(ctx, AddPermission.ID, params);

        assertNotNull(doc.getACP());

        ACL acl = doc.getACP().getACL("local");
        assertNotNull(acl);
        assertEquals(1, acl.size());

        assertExpectedPermissions(acl, params, "existingUser1");
    }

    protected void assertExpectedPermissions(ACL acl, Map<String, Object> params, String userOrGroup) {
        ACE ace = acl.stream().filter(el -> {
            String email = (String) params.get("email");
            if (Objects.nonNull(email) && email.equals(userOrGroup)) {
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
