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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.user.invite.RegistrationRules.FIELD_ALLOW_USER_CREATION;
import static org.nuxeo.ecm.user.invite.UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class TestMultipleConfiguration extends AbstractUserRegistration {

    @Test
    public void testMultipleConfigurationRegistration() {
        Map<String, UserRegistrationConfiguration> configurations = ((UserInvitationComponent) userRegistrationService).configurations;
        assertEquals(2, configurations.size());
        assertTrue(configurations.containsKey("test"));
        assertTrue(configurations.containsKey(DEFAULT_CONFIGURATION_NAME));
    }

    @Test
    public void testMultipleRegistrationRules() {
        initializeRegistrations();

        DocumentModel root = ((UserInvitationComponent) userRegistrationService).getOrCreateRootDocument(session,
                DEFAULT_CONFIGURATION_NAME);
        root.setPropertyValue(FIELD_ALLOW_USER_CREATION, false);
        session.saveDocument(root);
        session.save();

        RegistrationRules rules = userRegistrationService.getRegistrationRules(DEFAULT_CONFIGURATION_NAME);
        assertFalse(rules.allowUserCreation());

        rules = userRegistrationService.getRegistrationRules("test");
        assertTrue(rules.allowUserCreation());
    }

    @Test
    public void testMultipleUserRegistration() {
        initializeRegistrations();

        assertEquals(0, userManager.searchUsers("testUser").size());
        assertEquals(0, userManager.searchUsers("testUser2").size());

        UserRegistrationConfiguration configuration = ((UserInvitationComponent) userRegistrationService).configurations.get(
                DEFAULT_CONFIGURATION_NAME);

        // User info
        DocumentModel userInfo = session.createDocumentModel(configuration.getRequestDocType());
        userInfo.setPropertyValue("userinfo:login", "testUser");
        userInfo.setPropertyValue("userinfo:email", "dummy@test.com");

        // Invite first user with defautl conf
        String requestId = userRegistrationService.submitRegistrationRequest(DEFAULT_CONFIGURATION_NAME, userInfo,
                new HashMap<String, Serializable>(), UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        // Invite second user with test conf
        userInfo.setPropertyValue("userinfo:login", "testUser2");
        userInfo.setPropertyValue("userinfo:email", "dummy2@test.com");
        requestId = userRegistrationService.submitRegistrationRequest("test", userInfo,
                new HashMap<String, Serializable>(), UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        session.save();

        // Default registration container
        assertEquals(1, session.getChildren(new PathRef("/requests")).size());
        // Test registration container
        assertEquals(1, session.getChildren(new PathRef("/test-requests")).size());

        assertNotNull(userManager.getUserModel("testUser"));
        assertNotNull(userManager.getUserModel("testUser2"));
    }

    @Test
    public void testForceValidationForNonExistingUser() {
        initializeRegistrations();

        UserRegistrationConfiguration configuration = ((UserInvitationComponent) userRegistrationService).configurations.get(
                DEFAULT_CONFIGURATION_NAME);
        DocumentModel userInfo = session.createDocumentModel(configuration.getRequestDocType());
        userInfo.setPropertyValue("userinfo:login", "testUser");
        userInfo.setPropertyValue("userinfo:email", "dummy@test.com");

        String requestId = userRegistrationService.submitRegistrationRequest(DEFAULT_CONFIGURATION_NAME, userInfo,
                new HashMap<>(), UserInvitationService.ValidationMethod.NONE, false);
        DocumentModel request = session.getDocument(new IdRef(requestId));
        assertNull(request.getPropertyValue("registration:accepted"));

        try {
            Framework.getProperties().put(RegistrationRules.FORCE_VALIDATION_NON_EXISTING_USER_PROPERTY, "true");
            requestId = userRegistrationService.submitRegistrationRequest(DEFAULT_CONFIGURATION_NAME, userInfo,
                    new HashMap<>(), UserInvitationService.ValidationMethod.NONE, false);
            request = session.getDocument(new IdRef(requestId));
            assertTrue((Boolean) request.getPropertyValue("registration:accepted"));
        } finally {
            Framework.getProperties().remove(RegistrationRules.FORCE_VALIDATION_NON_EXISTING_USER_PROPERTY);
        }
    }
}
