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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.user.invite.UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;

import java.io.Serializable;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.user.invite.RegistrationRules;
import org.nuxeo.ecm.user.invite.UserRegistrationInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class TestMultipleConfiguration extends AbstractUserRegistration {

    protected DocumentModel testWorkspace;

    @Before
    public void createTestWorkspace() {
        testWorkspace = session.createDocumentModel("/default-domain", "testWorkspace", "Workspace");
        testWorkspace.setPropertyValue("dc:title", "Test Workspace");
        session.createDocument(testWorkspace);
    }

    @Test
    public void testMultipleUserRegistration() {
        initializeRegistrations();

        assertEquals(0, userManager.searchUsers("testUser").size());
        assertEquals(0, userManager.searchUsers("testUser2").size());

        // User info
        UserRegistrationInfo userInfo = new UserRegistrationInfo();
        userInfo.setLogin("testUser");
        userInfo.setEmail("dummy@test.com");

        // Doc info
        DocumentRegistrationInfo docInfo = new DocumentRegistrationInfo();
        docInfo.setDocumentId(testWorkspace.getId());
        docInfo.setPermission(SecurityConstants.READ_WRITE);

        // Invite first user with defautl conf
        String requestId = userRegistrationService.submitRegistrationRequest(DEFAULT_CONFIGURATION_NAME, userInfo,
                docInfo, new HashMap<String, Serializable>(), UserRegistrationService.ValidationMethod.NONE, true,
                "adminTest");
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        // Invite second user with test conf
        userInfo.setLogin("testUser2");
        requestId = userRegistrationService.submitRegistrationRequest("test", userInfo, docInfo,
                new HashMap<String, Serializable>(), UserRegistrationService.ValidationMethod.NONE, true, "adminTest");
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

        // User info
        UserRegistrationInfo userInfo = new UserRegistrationInfo();
        userInfo.setLogin("testUser");
        userInfo.setEmail("dummy@test.com");

        // Doc info
        DocumentRegistrationInfo docInfo = new DocumentRegistrationInfo();
        docInfo.setDocumentId(testWorkspace.getId());
        docInfo.setPermission(SecurityConstants.READ_WRITE);

        String requestId = userRegistrationService.submitRegistrationRequest("test", userInfo, docInfo, new HashMap<>(),
                UserRegistrationService.ValidationMethod.NONE, false, "adminTest");
        DocumentModel request = session.getDocument(new IdRef(requestId));
        assertNull(request.getPropertyValue("registration:accepted"));

        try {
            Framework.getProperties().put(RegistrationRules.FORCE_VALIDATION_NON_EXISTING_USER_PROPERTY, "true");
            requestId = userRegistrationService.submitRegistrationRequest("test", userInfo, docInfo, new HashMap<>(),
                    UserRegistrationService.ValidationMethod.NONE, false, "adminTest");
            request = session.getDocument(new IdRef(requestId));
            assertTrue((Boolean) request.getPropertyValue("registration:accepted"));
        } finally {
            Framework.getProperties().remove(RegistrationRules.FORCE_VALIDATION_NON_EXISTING_USER_PROPERTY);
        }
    }
}
