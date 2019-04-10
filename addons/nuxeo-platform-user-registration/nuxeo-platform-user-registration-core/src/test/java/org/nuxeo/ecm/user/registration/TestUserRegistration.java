package org.nuxeo.ecm.user.registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.user.registration" })
@LocalDeploy({ "org.nuxeo.ecm.user.registration:test-types-contrib.xml" })
public class TestUserRegistration {

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected UserRegistrationService userRegistrationService;

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
                UserRegistrationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId);

        assertEquals(1, userManager.searchUsers("jolivier").size());
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
                userInfo, docInfo, new HashMap<String, Serializable>(),
                UserRegistrationService.ValidationMethod.NONE, true);
        userRegistrationService.validateRegistration(requestId);

        session.save();

        // New user created
        assertEquals(1, userManager.searchUsers(userInfo.getLogin()).size());
        // ACL added
        assertEquals(2, session.getACP(testWorkspace.getRef()).getACLs().length);
        assertTrue(session.getACP(testWorkspace.getRef()).getAccess("testUser",
                SecurityConstants.READ_WRITE).toBoolean());
    }
}
