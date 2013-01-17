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

import static org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper.TEST_WORKSPACE_PATH;
import static org.nuxeo.drive.operations.test.NuxeoDriveIntegrationTestsHelper.USER_WORKSPACE_PARENT_PATH;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.operations.test.NuxeoDriveSetupIntegrationTests;
import org.nuxeo.drive.operations.test.NuxeoDriveTearDownIntegrationTests;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;

/**
 * Tests the Nuxeo Drive integration tests operations.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(RestFeature.class)
@Deploy("org.nuxeo.drive.operations")
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18080)
public class TestIntegrationTestOperations {

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected HttpAutomationClient automationClient;

    protected Session clientSession;

    protected ObjectMapper mapper;

    @Before
    public void init() throws Exception {

        // Get an Automation client session as Administrator
        clientSession = automationClient.getSession("Administrator",
                "Administrator");
        mapper = new ObjectMapper();
    }

    // TODO: unignore
    @Ignore
    @Test
    public void testIntegrationTestsSetupAndTearDown() throws Exception {

        // ---------------------------------------------------------
        // Setup the integration tests environment as Administrator
        // ---------------------------------------------------------
        Blob testUserPasswordsJSON = (Blob) clientSession.newRequest(
                NuxeoDriveSetupIntegrationTests.ID).set("userNames", "joe,jack").execute();
        assertNotNull(testUserPasswordsJSON);

        // Check test users
        String testUserPasswords = mapper.readValue(
                testUserPasswordsJSON.getStream(), String.class);
        assertNotNull(testUserPasswords);
        String[] testUserPasswordsArray = StringUtils.split(testUserPasswords,
                ",");
        assertEquals(2, testUserPasswordsArray.length);

        NuxeoPrincipal joePrincipal = userManager.getPrincipal("nuxeoDriveTestUser_joe");
        assertNotNull(joePrincipal);
        assertTrue(joePrincipal.getGroups().contains("members"));
        NuxeoPrincipal jackPrincipal = userManager.getPrincipal("nuxeoDriveTestUser_jack");
        assertNotNull(jackPrincipal);
        assertTrue(jackPrincipal.getGroups().contains("members"));

        // Check test workspace
        DocumentRef testWorkspaceRef = new PathRef(TEST_WORKSPACE_PATH);
        DocumentModel testWorkspace = session.getDocument(testWorkspaceRef);
        assertEquals("Workspace", testWorkspace.getType());
        assertEquals("Nuxeo Drive Test Workspace", testWorkspace.getTitle());
        assertTrue(session.hasPermission(joePrincipal, testWorkspaceRef,
                SecurityConstants.WRITE));
        assertTrue(session.hasPermission(jackPrincipal, testWorkspaceRef,
                SecurityConstants.WRITE));

        // ----------------------------------------------------------------------
        // Setup the integration tests environment with other user names without
        // having teared it down previously => should start by cleaning it up
        // ----------------------------------------------------------------------
        testUserPasswordsJSON = (Blob) clientSession.newRequest(
                NuxeoDriveSetupIntegrationTests.ID).set("userNames", "sarah").execute();
        assertNotNull(testUserPasswordsJSON);

        // Check test users
        testUserPasswords = mapper.readValue(testUserPasswordsJSON.getStream(),
                String.class);
        assertNotNull(testUserPasswords);
        testUserPasswordsArray = StringUtils.split(testUserPasswords, ",");
        assertEquals(1, testUserPasswordsArray.length);

        assertNull(userManager.getPrincipal("nuxeoDriveTestUser_joe"));
        assertNull(userManager.getPrincipal("nuxeoDriveTestUser_jack"));
        assertFalse(session.exists(new PathRef(USER_WORKSPACE_PARENT_PATH
                + "/nuxeoDriveTestUser_joe")));
        assertFalse(session.exists(new PathRef(USER_WORKSPACE_PARENT_PATH
                + "/nuxeoDriveTestUser_jack")));

        NuxeoPrincipal sarahPrincipal = userManager.getPrincipal("nuxeoDriveTestUser_sarah");
        assertNotNull(sarahPrincipal);
        assertTrue(sarahPrincipal.getGroups().contains("members"));

        // Check test workspace
        testWorkspace = session.getDocument(testWorkspaceRef);
        assertEquals("Nuxeo Drive Test Workspace", testWorkspace.getTitle());
        // TODO: fix
        // assertTrue(session.hasPermission(sarahPrincipal, testWorkspaceRef,
        // SecurityConstants.WRITE));

        // ----------------------------------------------------------------------
        // Try to setup the integration tests environment as an unauthorized
        // user => should fail
        // ----------------------------------------------------------------------
        // TODO: fix
        // Session unauthorizedSession = automationClient.getSession(
        // "nuxeoDriveTestUser_sarah", testUserPasswordsArray[0]);
        // try {
        // unauthorizedSession.newRequest(NuxeoDriveSetupIntegrationTests.ID).set(
        // "userNames", "john,bob").execute();
        // fail("NuxeoDrive.SetupIntegrationTests operation should not be callable by a non administrator.");
        // } catch (Exception e) {
        // // Expected
        // }

        // ----------------------------------------------------------------------
        // Try to tear down the integration tests environment as an unauthorized
        // user => should fail
        // ----------------------------------------------------------------------
        // TODO: fix
        // try {
        // unauthorizedSession.newRequest(
        // NuxeoDriveTearDownIntegrationTests.ID).execute();
        // fail("NuxeoDrive.TearDownIntegrationTests operation should not be callable by a non administrator.");
        // } catch (Exception e) {
        // // Expected
        // }

        // ----------------------------------------------------------------------
        // Tear down the integration tests environment as Administrator
        // ----------------------------------------------------------------------
        clientSession.newRequest(NuxeoDriveTearDownIntegrationTests.ID).execute();
        session.save();
        assertTrue(userManager.searchUsers("nuxeoDriveTestUser_").isEmpty());
        assertFalse(session.exists(new PathRef(USER_WORKSPACE_PARENT_PATH
                + "/nuxeoDriveTestUser_sarah")));
        assertFalse(session.exists(testWorkspaceRef));
    }
}
