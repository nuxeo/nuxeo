/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.ClientException;
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
    public void init() throws ClientException {
        initializeRegistrations();
    }

    @Test
    public void testTestContribution() throws ClientException {
        DocumentModel doc = session.createDocumentModel("TestRegistration");
        assertTrue(doc.hasFacet("UserRegistration"));

        assertNotNull(userRegistrationService);
        UserRegistrationConfiguration config = userRegistrationService.getConfiguration();
        assertEquals("Workspace", config.getContainerDocType());
    }

    @Test
    public void testBasicUserRegistration() throws ClientException {
        UserRegistrationInfo userInfo = new UserRegistrationInfo();
        userInfo.setLogin("jolivier");
        userInfo.setFirstName("John");
        userInfo.setLastName("Olivier");
        userInfo.setEmail("oolivier@dummy.com");

        assertEquals(0, userManager.searchUsers("jolivier").size());

        String requestId = userRegistrationService.submitRegistrationRequest(
                userInfo, new HashMap<String, Serializable>(),
                UserRegistrationService.ValidationMethod.NONE, true, "adminTest");
        userRegistrationService.validateRegistration(requestId,
                new HashMap<String, Serializable>());

        assertEquals(1, userManager.searchUsers("jolivier").size());
    }

    @Test
    public void testBasicUserRegistrationWithLoginChanged()
            throws ClientException {
        UserRegistrationInfo userInfo = new UserRegistrationInfo();
        String templogin = "templogin";
        String newUser = "newUser";

        userInfo.setLogin(templogin);
        userInfo.setFirstName("John");
        userInfo.setLastName("Olivier");
        userInfo.setEmail("oolivier@dummy.com");

        assertEquals(0, userManager.searchUsers(templogin).size());
        assertEquals(0, userManager.searchUsers(newUser).size());

        String requestId = userRegistrationService.submitRegistrationRequest(
                userInfo, new HashMap<String, Serializable>(),
                UserRegistrationService.ValidationMethod.NONE, true, "adminTest");
        Map<String, Serializable> additionnalInfos = new HashMap<String, Serializable>();
        additionnalInfos.put("userinfo:login", newUser);
        userRegistrationService.validateRegistration(requestId,
                additionnalInfos);

        assertEquals(0, userManager.searchUsers(templogin).size());
        assertEquals(1, userManager.searchUsers(newUser).size());
    }

    @Test
    public void testUserRegistrationWithDocument() throws ClientException {
        DocumentModel testWorkspace = session.createDocumentModel(
                "/default-domain", "testWorkspace", "Workspace");
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
        assertFalse(session.getACP(testWorkspace.getRef()).getAccess(
                "testUser", SecurityConstants.READ_WRITE).toBoolean());

        String requestId = userRegistrationService.submitRegistrationRequest(
                DEFAULT_CONFIGURATION_NAME, userInfo, docInfo,
                new HashMap<String, Serializable>(),
                UserRegistrationService.ValidationMethod.NONE, true, "adminTest");
        userRegistrationService.validateRegistration(requestId,
                new HashMap<String, Serializable>());

        session.save();

        // New user created
        assertEquals(1, userManager.searchUsers(userInfo.getLogin()).size());
        // ACL added
        assertEquals(2, session.getACP(testWorkspace.getRef()).getACLs().length);
        assertTrue(session.getACP(testWorkspace.getRef()).getAccess("testUser",
                SecurityConstants.READ_WRITE).toBoolean());

        String searchUserRegistration = "Select * from Document where ecm:mixinType = 'UserRegistration'";
        assertEquals(1, session.query(searchUserRegistration).size());

        // check cleanup
        session.removeDocument(testWorkspace.getRef());
        session.save();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        assertEquals(0, session.query(searchUserRegistration).size());
    }
}
