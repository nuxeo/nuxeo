/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper;
import org.nuxeo.drive.operations.test.NuxeoDriveSetupIntegrationTests;
import org.nuxeo.drive.operations.test.NuxeoDriveTearDownIntegrationTests;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Tests the Nuxeo Drive integration tests operations.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, EmbeddedAutomationServerFeature.class})
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.user.center.profile", "org.nuxeo.drive.operations" })
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
@Jetty(port = 18080)
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

        testWorkspacePath = NuxeoDriveIntegrationTestsHelper.getDefaultDomainPath(session)
                + "/" + TEST_WORKSPACE_PARENT_NAME + "/" + TEST_WORKSPACE_NAME;
        userWorkspaceParentPath = NuxeoDriveIntegrationTestsHelper.getDefaultDomainPath(session)
                + "/" + USER_WORKSPACE_PARENT_NAME;

        mapper = new ObjectMapper();
    }

    @Test
    public void testIntegrationTestsSetupAndTearDown() throws Exception {

        // ---------------------------------------------------------
        // Setup the integration tests environment as Administrator
        // ---------------------------------------------------------
        Blob testUserCredentialsBlob = (Blob) clientSession.newRequest(
                NuxeoDriveSetupIntegrationTests.ID).set("userNames", "joe,jack").set(
                "permission", "ReadWrite").execute();
        assertNotNull(testUserCredentialsBlob);
        // Invalidate VCS cache
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Check test users
        String testUserCredentials = IOUtils.toString(
                testUserCredentialsBlob.getStream(), "UTF-8");
        assertNotNull(testUserCredentials);
        String[] testUserCrendentialsArray = StringUtils.split(
                testUserCredentials, ",");
        assertEquals(2, testUserCrendentialsArray.length);
        assertTrue(testUserCrendentialsArray[0].startsWith("nuxeoDriveTestUser_joe:"));
        assertTrue(testUserCrendentialsArray[1].startsWith("nuxeoDriveTestUser_jack:"));

        // useMembersGroup is false by default
        NuxeoPrincipal joePrincipal = userManager.getPrincipal("nuxeoDriveTestUser_joe");
        assertNotNull(joePrincipal);
        assertFalse(joePrincipal.getGroups().contains("members"));
        NuxeoPrincipal jackPrincipal = userManager.getPrincipal("nuxeoDriveTestUser_jack");
        assertNotNull(jackPrincipal);
        assertFalse(jackPrincipal.getGroups().contains("members"));

        // Check test workspace
        DocumentRef testWorkspaceRef = new PathRef(testWorkspacePath);
        DocumentModel testWorkspace = session.getDocument(testWorkspaceRef);
        assertEquals("Workspace", testWorkspace.getType());
        assertEquals("Nuxeo Drive Test Workspace", testWorkspace.getTitle());
        assertTrue(session.hasPermission(joePrincipal, testWorkspaceRef,
                SecurityConstants.WRITE));
        assertTrue(session.hasPermission(jackPrincipal, testWorkspaceRef,
                SecurityConstants.WRITE));

        // Create test users' personal workspaces for cleanup check
        userWorkspaceService.getUserPersonalWorkspace("nuxeoDriveTestUser_joe",
                session.getRootDocument());
        userWorkspaceService.getUserPersonalWorkspace(
                "nuxeoDriveTestUser_jack", session.getRootDocument());
        assertNotNull(session.getDocument(new PathRef(userWorkspaceParentPath
                + "/nuxeoDriveTestUser-joe")));
        assertNotNull(session.getDocument(new PathRef(userWorkspaceParentPath
                + "/nuxeoDriveTestUser-jack")));
        // Save personal workspaces
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ----------------------------------------------------------------------
        // Setup the integration tests environment with other user names without
        // having teared it down previously => should start by cleaning it up
        // ----------------------------------------------------------------------
        testUserCredentialsBlob = (Blob) clientSession.newRequest(
                NuxeoDriveSetupIntegrationTests.ID).set("userNames", "sarah").set(
                "useMembersGroup", true).set("permission", "ReadWrite").execute();
        assertNotNull(testUserCredentialsBlob);

        // Check cleanup
        assertNull(userManager.getPrincipal("nuxeoDriveTestUser_joe"));
        assertNull(userManager.getPrincipal("nuxeoDriveTestUser_jack"));
        // Process invalidations
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        try {
            session.getDocument(new PathRef(userWorkspaceParentPath
                    + "/nuxeoDriveTestUser-joe"));
            fail("User workspace should not exist.");
        } catch (ClientException e) {
            assertEquals("Failed to get document " + userWorkspaceParentPath
                    + "/nuxeoDriveTestUser-joe", e.getMessage());
        }
        try {
            session.getDocument(new PathRef(userWorkspaceParentPath
                    + "/nuxeoDriveTestUser-jack"));
            fail("User workspace should not exist.");
        } catch (ClientException e) {
            assertEquals("Failed to get document " + userWorkspaceParentPath
                    + "/nuxeoDriveTestUser-jack", e.getMessage());
        }

        // Check test users
        testUserCredentials = IOUtils.toString(
                testUserCredentialsBlob.getStream(), "UTF-8");
        assertNotNull(testUserCredentials);
        testUserCrendentialsArray = StringUtils.split(testUserCredentials, ",");
        assertEquals(1, testUserCrendentialsArray.length);
        assertTrue(testUserCrendentialsArray[0].startsWith("nuxeoDriveTestUser_sarah:"));

        NuxeoPrincipal sarahPrincipal = userManager.getPrincipal("nuxeoDriveTestUser_sarah");
        assertNotNull(sarahPrincipal);
        assertTrue(sarahPrincipal.getGroups().contains("members"));

        // Check test workspace
        testWorkspace = session.getDocument(testWorkspaceRef);
        assertEquals("Nuxeo Drive Test Workspace", testWorkspace.getTitle());
        assertTrue(session.hasPermission(sarahPrincipal, testWorkspaceRef,
                SecurityConstants.WRITE));

        // Create test users' personal workspaces for cleanup check
        userWorkspaceService.getUserPersonalWorkspace(
                "nuxeoDriveTestUser_sarah", session.getRootDocument());
        assertNotNull(session.getDocument(new PathRef(userWorkspaceParentPath
                + "/nuxeoDriveTestUser-sarah")));
        // Save personal workspaces
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // ----------------------------------------------------------------------
        // Try to setup the integration tests environment as an unauthorized
        // user => should fail
        // ----------------------------------------------------------------------
        String sarahCredentials = testUserCrendentialsArray[0];
        String sarahPassword = sarahCredentials.substring(sarahCredentials.indexOf(':') + 1);
        Session unauthorizedSession = automationClient.getSession(
                "nuxeoDriveTestUser_sarah", sarahPassword);
        try {
            unauthorizedSession.newRequest(NuxeoDriveSetupIntegrationTests.ID).set(
                    "userNames", "john,bob").set("permission", "ReadWrite").execute();
            fail("NuxeoDrive.SetupIntegrationTests operation should not be callable by a non administrator.");
        } catch (Exception e) {
            // Expected
        }

        // ----------------------------------------------------------------------
        // Try to tear down the integration tests environment as an unauthorized
        // user => should fail
        // ----------------------------------------------------------------------
        try {
            unauthorizedSession.newRequest(
                    NuxeoDriveTearDownIntegrationTests.ID).execute();
            fail("NuxeoDrive.TearDownIntegrationTests operation should not be callable by a non administrator.");
        } catch (Exception e) {
            // Expected
        }

        // ----------------------------------------------------------------------
        // Tear down the integration tests environment as Administrator
        // ----------------------------------------------------------------------
        clientSession.newRequest(NuxeoDriveTearDownIntegrationTests.ID).execute();
        assertTrue(userManager.searchUsers("nuxeoDriveTestUser_").isEmpty());
        // Process invalidations
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        try {
            session.getDocument(new PathRef(userWorkspaceParentPath
                    + "/nuxeoDriveTestUser-sarah"));
            fail("User workspace should not exist.");
        } catch (ClientException e) {
            assertEquals("Failed to get document " + userWorkspaceParentPath
                    + "/nuxeoDriveTestUser-sarah", e.getMessage());
        }
        assertFalse(session.exists(testWorkspaceRef));
    }

}
