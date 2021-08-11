/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.test;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.services.UserInvite;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.user.invite")
@Deploy("org.nuxeo.ecm.user.invite:test-invite-contrib.xml")
public class UserInviteTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    UserManager um;

    protected OperationContext ctx;

    @Before
    public void init() {
        DocumentModel container = session.createDocumentModel("Workspace");
        container.setPathInfo("/", "requests");
        session.createDocument(container);
        session.save();
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Test
    public void testInviteUser() throws Exception {
        NuxeoPrincipal user = new NuxeoPrincipalImpl("user");
        ctx.setInput(user);

        String invitationId = (String) service.run(ctx, UserInvite.ID, Collections.emptyMap());

        DocumentModel doc = session.getDocument(new IdRef(invitationId));
        assertEquals(user.getName(), doc.getPropertyValue("userinfo:login"));
    }

    @Test
    public void testInviteExistingUserException() throws Exception {
        // Given a user
        NuxeoPrincipal testUser = new NuxeoPrincipalImpl("testUser");
        um.createUser(testUser.getModel());

        // When trying to invite the existing user
        ctx.setInput(testUser);
        try {
            service.run(ctx, UserInvite.ID, Collections.emptyMap());
            fail("User.Invite should have failed with an existent user");
        } catch (UserAlreadyExistsException e) {
            // Should return the UserAlreadyExists Exception
            assertEquals(SC_CONFLICT, e.getStatusCode());
        }
    }
}
