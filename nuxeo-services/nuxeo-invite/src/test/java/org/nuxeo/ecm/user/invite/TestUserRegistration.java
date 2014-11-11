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
package org.nuxeo.ecm.user.invite;

import static org.junit.Assert.assertEquals;
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
        assertTrue(doc.hasFacet("UserInvitation"));

        assertNotNull(userRegistrationService);
        UserRegistrationConfiguration config = userRegistrationService.getConfiguration();
        assertEquals("Workspace", config.getContainerDocType());
    }

    @Test
    public void testBasicUserRegistration() throws ClientException {
        UserRegistrationConfiguration configuration = ((UserInvitationComponent) userRegistrationService).configurations.get(DEFAULT_CONFIGURATION_NAME);
        // User info
        DocumentModel userInfo = session.createDocumentModel(configuration.getRequestDocType());
        userInfo.setPropertyValue("userinfo:login", "jolivier");
        userInfo.setPropertyValue("userinfo:firstName", "jolivier");
        userInfo.setPropertyValue("userinfo:lastName", "jolivier");
        userInfo.setPropertyValue("userinfo:email", "oolivier@dummy.com");

        assertEquals(0, userManager.searchUsers("jolivier").size());

        String requestId = userRegistrationService.submitRegistrationRequest(
                userInfo, new HashMap<String, Serializable>(),
                UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId,
                new HashMap<String, Serializable>());

        assertEquals(1, userManager.searchUsers("jolivier").size());
    }

    @Test
    public void testBasicUserRegistrationWithLoginChanged()
            throws ClientException {
        UserRegistrationConfiguration configuration = ((UserInvitationComponent) userRegistrationService).configurations.get(DEFAULT_CONFIGURATION_NAME);
        // User info
        DocumentModel userInfo = session.createDocumentModel(configuration.getRequestDocType());
        String templogin = "templogin";
        String newUser = "newUser";

        userInfo.setPropertyValue("userinfo:login", templogin);
        userInfo.setPropertyValue("userinfo:firstName", "John");
        userInfo.setPropertyValue("userinfo:lastName", "Olivier");
        userInfo.setPropertyValue("userinfo:email", "oolivier@dummy.com");

        assertEquals(0, userManager.searchUsers(templogin).size());
        assertEquals(0, userManager.searchUsers(newUser).size());

        String requestId = userRegistrationService.submitRegistrationRequest(
                userInfo, new HashMap<String, Serializable>(),
                UserInvitationService.ValidationMethod.NONE, true);
        Map<String, Serializable> additionnalInfos = new HashMap<String, Serializable>();
        additionnalInfos.put("userinfo:login", newUser);
        userRegistrationService.validateRegistration(requestId,
                additionnalInfos);

        assertEquals(0, userManager.searchUsers(templogin).size());
        assertEquals(1, userManager.searchUsers(newUser).size());
    }
}
