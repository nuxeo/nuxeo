/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper.TEST_WORKSPACE_NAME;
import static org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper.TEST_WORKSPACE_PARENT_NAME;
import static org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper.USER_WORKSPACE_PARENT_NAME;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper;
import org.nuxeo.drive.operations.test.NuxeoDriveSetupIntegrationTests;
import org.nuxeo.drive.operations.test.NuxeoDriveTearDownIntegrationTests;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the Nuxeo Drive integration tests operations.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveAutomationFeature.class)
@ServletContainer(port = 18080)
public class TestIntegrationTestOperations {

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected UserWorkspaceService userWorkspaceService;

    @Inject
    protected HttpAutomationClient automationClient;

    @Inject
    protected Session clientSession;

    protected String testWorkspacePath;

    protected String userWorkspaceParentPath;

    protected ObjectMapper mapper;

    @Before
    public void init() throws Exception {

        testWorkspacePath = NuxeoDriveIntegrationTestsHelper.getDefaultDomainPath(session) + "/"
                + TEST_WORKSPACE_PARENT_NAME + "/" + TEST_WORKSPACE_NAME;
        userWorkspaceParentPath = NuxeoDriveIntegrationTestsHelper.getDefaultDomainPath(session) + "/"
                + USER_WORKSPACE_PARENT_NAME;

        mapper = new ObjectMapper();
    }

    @Test
    public void testIntegrationTestsSetupAndTearDown() throws Exception {

        // ---------------------------------------------------------
        // Setup the integration tests environment as Administrator
        // ---------------------------------------------------------
        Blob testUserCredentialsBlob = (Blob) clientSession.newRequest(NuxeoDriveSetupIntegrationTests.ID)
                                                           .set("userNames", "joe,jack")
                                                           .set("permission", "ReadWrite")
                                                           .execute();
        assertNotNull(testUserCredentialsBlob);
        // Invalidate VCS cache
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Check test users
        String testUserCredentials = IOUtils.toString(testUserCredentialsBlob.getStream(), "UTF-8");
        assertNotNull(testUserCredentials);
        String[] testUserCrendentialsArray = StringUtils.split(testUserCredentials, ",");
        assertEquals(2, testUserCrendentialsArray.length);
        assertTrue(testUserCrendentialsArray[0].startsWith("drivejoe:"));
        assertTrue(testUserCrendentialsArray[1].startsWith("drivejack:"));

        // useMembersGroup is false by default
        NuxeoPrincipal joePrincipal = userManager.getPrincipal("drivejoe");
        assertNotNull(joePrincipal);
        assertFalse(joePrincipal.getGroups().contains("members"));
        NuxeoPrincipal jackPrincipal = userManager.getPrincipal("drivejack");
        assertNotNull(jackPrincipal);
        assertFalse(jackPrincipal.getGroups().contains("members"));

        // Check test workspace
        DocumentRef testWorkspaceRef = new PathRef(testWorkspacePath);
        DocumentModel testWorkspace = session.getDocument(testWorkspaceRef);
        assertEquals("Workspace", testWorkspace.getType());
        assertEquals("Nuxeo Drive Test Workspace", testWorkspace.getTitle());
        assertTrue(session.hasPermission(joePrincipal, testWorkspaceRef, SecurityConstants.WRITE));
        assertTrue(session.hasPermission(jackPrincipal, testWorkspaceRef, SecurityConstants.WRITE));

        // Create test users' personal workspaces for cleanup check
        userWorkspaceService.getUserPersonalWorkspace("drivejoe", session.getRootDocument());
        userWorkspaceService.getUserPersonalWorkspace("drivejack", session.getRootDocument());
        assertNotNull(session.getDocument(new PathRef(userWorkspaceParentPath + "/drivejoe")));
        assertNotNull(session.getDocument(new PathRef(userWorkspaceParentPath + "/drivejack")));
        // Save personal workspaces
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ----------------------------------------------------------------------
        // Setup the integration tests environment with other user names without
        // having teared it down previously => should start by cleaning it up
        // ----------------------------------------------------------------------
        testUserCredentialsBlob = (Blob) clientSession.newRequest(NuxeoDriveSetupIntegrationTests.ID)
                                                      .set("userNames", "sarah")
                                                      .set("useMembersGroup", true)
                                                      .set("permission", "ReadWrite")
                                                      .execute();
        assertNotNull(testUserCredentialsBlob);

        // Check cleanup
        assertNull(userManager.getPrincipal("drivejoe"));
        assertNull(userManager.getPrincipal("drivejack"));
        // Process invalidations
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        try {
            session.getDocument(new PathRef(userWorkspaceParentPath + "/drivejoe"));
            fail("User workspace should not exist.");
        } catch (DocumentNotFoundException e) {
            assertEquals(userWorkspaceParentPath + "/drivejoe", e.getMessage());
        }
        try {
            session.getDocument(new PathRef(userWorkspaceParentPath + "/drivejack"));
            fail("User workspace should not exist.");
        } catch (DocumentNotFoundException e) {
            assertEquals(userWorkspaceParentPath + "/drivejack", e.getMessage());
        }

        // Check test users
        testUserCredentials = IOUtils.toString(testUserCredentialsBlob.getStream(), "UTF-8");
        assertNotNull(testUserCredentials);
        testUserCrendentialsArray = StringUtils.split(testUserCredentials, ",");
        assertEquals(1, testUserCrendentialsArray.length);
        assertTrue(testUserCrendentialsArray[0].startsWith("drivesarah:"));

        NuxeoPrincipal sarahPrincipal = userManager.getPrincipal("drivesarah");
        assertNotNull(sarahPrincipal);
        assertTrue(sarahPrincipal.getGroups().contains("members"));

        // Check test workspace
        testWorkspace = session.getDocument(testWorkspaceRef);
        assertEquals("Nuxeo Drive Test Workspace", testWorkspace.getTitle());
        assertTrue(session.hasPermission(sarahPrincipal, testWorkspaceRef, SecurityConstants.WRITE));

        // Create test users' personal workspaces for cleanup check
        userWorkspaceService.getUserPersonalWorkspace("drivesarah", session.getRootDocument());
        assertNotNull(session.getDocument(new PathRef(userWorkspaceParentPath + "/drivesarah")));
        // Save personal workspaces
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ----------------------------------------------------------------------
        // Try to setup the integration tests environment as an unauthorized
        // user => should fail
        // ----------------------------------------------------------------------
        String sarahCredentials = testUserCrendentialsArray[0];
        String sarahPassword = sarahCredentials.substring(sarahCredentials.indexOf(':') + 1);
        Session unauthorizedSession = automationClient.getSession("drivesarah", sarahPassword);
        try {
            unauthorizedSession.newRequest(NuxeoDriveSetupIntegrationTests.ID)
                               .set("userNames", "john,bob")
                               .set("permission", "ReadWrite")
                               .execute();
            fail("NuxeoDrive.SetupIntegrationTests operation should not be callable by a non administrator.");
        } catch (Exception e) {
            // Expected
        }

        // ----------------------------------------------------------------------
        // Try to tear down the integration tests environment as an unauthorized
        // user => should fail
        // ----------------------------------------------------------------------
        try {
            unauthorizedSession.newRequest(NuxeoDriveTearDownIntegrationTests.ID).execute();
            fail("NuxeoDrive.TearDownIntegrationTests operation should not be callable by a non administrator.");
        } catch (Exception e) {
            // Expected
        }

        // ----------------------------------------------------------------------
        // Tear down the integration tests environment as Administrator
        // ----------------------------------------------------------------------
        clientSession.newRequest(NuxeoDriveTearDownIntegrationTests.ID).execute();
        assertTrue(userManager.searchUsers("drive").isEmpty());
        // Process invalidations
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        try {
            session.getDocument(new PathRef(userWorkspaceParentPath + "/drivesarah"));
            fail("User workspace should not exist.");
        } catch (DocumentNotFoundException e) {
            assertEquals(userWorkspaceParentPath + "/drivesarah", e.getMessage());
        }
        assertFalse(session.exists(testWorkspaceRef));
    }

}
