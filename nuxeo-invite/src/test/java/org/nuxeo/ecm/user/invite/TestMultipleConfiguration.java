package org.nuxeo.ecm.user.invite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.user.invite.RegistrationRules.FIELD_ALLOW_USER_CREATION;
import static org.nuxeo.ecm.user.invite.UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

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
    public void testMultipleRegistrationRules() throws ClientException {
        initializeRegistrations();

        DocumentModel root = ((UserInvitationComponent) userRegistrationService).getOrCreateRootDocument(
                session, DEFAULT_CONFIGURATION_NAME);
        root.setPropertyValue(FIELD_ALLOW_USER_CREATION, false);
        session.saveDocument(root);
        session.save();

        RegistrationRules rules = userRegistrationService.getRegistrationRules(DEFAULT_CONFIGURATION_NAME);
        assertFalse(rules.allowUserCreation());

        rules = userRegistrationService.getRegistrationRules("test");
        assertTrue(rules.allowUserCreation());
    }

    @Test
    public void testMultipleUserRegistration() throws ClientException {
        initializeRegistrations();

        // Create workspaces where users will be invited
        DocumentModel testWorkspace = session.createDocumentModel(
                "/default-domain", "testWorkspace", "Workspace");
        testWorkspace.setPropertyValue("dc:title", "Test Workspace");
        String workspaceId = session.createDocument(testWorkspace).getId();
        session.save();

        assertEquals(0, userManager.searchUsers("testUser").size());
        assertEquals(0, userManager.searchUsers("testUser2").size());

        UserRegistrationConfiguration configuration = ((UserInvitationComponent) userRegistrationService).configurations.get(DEFAULT_CONFIGURATION_NAME);

        // User info
        DocumentModel userInfo = session.createDocumentModel(configuration.getRequestDocType());
        userInfo.setPropertyValue("userinfo:login","testUser");
        userInfo.setPropertyValue("userinfo:email","dummy@test.com");


        // Invite first user with defautl conf
        String requestId = userRegistrationService.submitRegistrationRequest(
                DEFAULT_CONFIGURATION_NAME, userInfo,
                new HashMap<String, Serializable>(),
                UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        // Invite second user with test conf
        userInfo.setPropertyValue("userinfo:login","testUser2");
        requestId = userRegistrationService.submitRegistrationRequest("test",
                userInfo, new HashMap<String, Serializable>(),
                UserInvitationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId, new HashMap<String, Serializable>());

        session.save();

        // Default registration container
        assertEquals(1, session.getChildren(new PathRef("/requests")).size());
        // Test registration container
        assertEquals(1, session.getChildren(new PathRef("/test-requests")).size());

        assertNotNull(userManager.getUserModel("testUser"));
        assertNotNull(userManager.getUserModel("testUser2"));
    }
}
