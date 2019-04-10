/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.user.registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.user.invite.UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.user.invite.UserRegistrationConfiguration;
import org.nuxeo.ecm.user.invite.UserRegistrationInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class TestUserRegistration extends AbstractUserRegistration {

    @Before
    public void init() {
        initializeRegistrations();
    }

    @Test
    public void testTestContribution() {
        DocumentModel doc = session.createDocumentModel("TestRegistration");
        assertTrue(doc.hasFacet("UserRegistration"));

        assertNotNull(userRegistrationService);
        UserRegistrationConfiguration config = userRegistrationService.getConfiguration();
        assertEquals("Workspace", config.getContainerDocType());
    }

    @Test
    public void testBasicUserRegistration() {
        UserRegistrationInfo userInfo = new UserRegistrationInfo();
        userInfo.setLogin("jolivier");
        userInfo.setFirstName("John");
        userInfo.setLastName("Olivier");
        userInfo.setEmail("oolivier@dummy.com");

        assertEquals(0, userManager.searchUsers("jolivier").size());

        String requestId = userRegistrationService.submitRegistrationRequest(userInfo,
                buildAdditionalInfo(), UserRegistrationService.ValidationMethod.NONE, true, "adminTest");
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        assertEquals(1, userManager.searchUsers("jolivier").size());
    }

    @Test
    public void testBasicUserRegistrationWithLoginChanged() {
        UserRegistrationInfo userInfo = new UserRegistrationInfo();
        String templogin = "templogin";
        String newUser = "newUser";

        userInfo.setLogin(templogin);
        userInfo.setFirstName("John");
        userInfo.setLastName("Olivier");
        userInfo.setEmail("oolivier@dummy.com");

        assertEquals(0, userManager.searchUsers(templogin).size());
        assertEquals(0, userManager.searchUsers(newUser).size());

        String requestId = userRegistrationService.submitRegistrationRequest(userInfo,
                buildAdditionalInfo(), UserRegistrationService.ValidationMethod.NONE, true, "adminTest");
        Map<String, Serializable> additionnalInfos = new HashMap<String, Serializable>();
        additionnalInfos.put("userinfo:login", newUser);
        userRegistrationService.validateRegistration(requestId, additionnalInfos);

        assertEquals(0, userManager.searchUsers(templogin).size());
        assertEquals(1, userManager.searchUsers(newUser).size());
    }

    @Test
    public void testUserRegistrationWithDocument() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain", "testWorkspace", "Workspace");
        testWorkspace.setPropertyValue("dc:title", "Test Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        session.save();

        assertEquals(1, session.getACP(testWorkspace.getRef()).getACLs().length); // inherited
                                                                                  // one

        UserRegistrationInfo userInfo = new UserRegistrationInfo();
        userInfo.setLogin("testUser");
        userInfo.setEmail("dummy@test.com");

        DocumentRegistrationInfo docInfo = new DocumentRegistrationInfo();
        docInfo.setDocumentId(testWorkspace.getId());
        docInfo.setPermission(SecurityConstants.READ_WRITE);

        assertEquals(0, userManager.searchUsers(userInfo.getLogin()).size());
        assertFalse(session.getACP(testWorkspace.getRef()).getAccess("testUser", SecurityConstants.READ_WRITE).toBoolean());

        String requestId = userRegistrationService.submitRegistrationRequest(DEFAULT_CONFIGURATION_NAME, userInfo,
                docInfo, buildAdditionalInfo(), UserRegistrationService.ValidationMethod.NONE, true,
                "adminTest");
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        session.save();

        // New user created
        assertEquals(1, userManager.searchUsers(userInfo.getLogin()).size());
        // ACL added
        assertEquals(2, session.getACP(testWorkspace.getRef()).getACLs().length);
        assertTrue(session.getACP(testWorkspace.getRef()).getAccess("testUser", SecurityConstants.READ_WRITE).toBoolean());

        String searchUserRegistration = "Select * from Document where ecm:mixinType = 'UserRegistration'";
        assertEquals(1, session.query(searchUserRegistration).size());

        // check cleanup
        session.removeDocument(testWorkspace.getRef());
        session.save();
        Framework.getService(EventService.class).waitForAsyncCompletion();

        assertEquals(0, session.query(searchUserRegistration).size());
    }
}
