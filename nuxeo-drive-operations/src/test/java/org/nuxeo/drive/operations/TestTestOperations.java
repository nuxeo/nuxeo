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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.operations.test.NuxeoDriveCreateTestUser;
import org.nuxeo.drive.operations.test.NuxeoDriveDeleteTestUser;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;

/**
 * Tests the test purpose operations.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(RestFeature.class)
@Deploy("org.nuxeo.drive.operations")
@RepositoryConfig(cleanup = Granularity.METHOD)
@Jetty(port = 18080)
public class TestTestOperations {

    @Inject
    protected HttpAutomationClient automationClient;

    @Inject
    protected UserManager userManager;

    protected Session clientSession;

    protected ObjectMapper mapper;

    @Before
    public void init() throws Exception {

        // Get an Automation client session as Administrator
        clientSession = automationClient.getSession("Administrator",
                "Administrator");
        mapper = new ObjectMapper();
    }

    @Test
    public void testCreateDeleteTestUser() throws Exception {

        // Create the nuxeoDriveTestUser as Administrator
        Blob testUserPasswordJSON = (Blob) clientSession.newRequest(
                NuxeoDriveCreateTestUser.ID).execute();
        assertNotNull(testUserPasswordJSON);

        String testUserPassword = mapper.readValue(
                testUserPasswordJSON.getStream(), String.class);
        assertNotNull(testUserPassword);
        NuxeoPrincipal newPrincipal = userManager.getPrincipal("nuxeoDriveTestUser");
        assertNotNull(newPrincipal);
        assertTrue(newPrincipal.getGroups().contains("members"));

        // Try to create a user that exists => should fail
        try {
            clientSession.newRequest(NuxeoDriveCreateTestUser.ID).execute();
            fail("NuxeoDrive.CreateTestUser operation should fail if user exists.");
        } catch (Exception e) {
            // Expected
        }

        // Try to create a test user as an unauthorized user => should fail
        Session unauthorizedSession = automationClient.getSession(
                "nuxeoDriveTestUser", testUserPassword);
        try {
            unauthorizedSession.newRequest(NuxeoDriveCreateTestUser.ID).set(
                    "userName", "joe").execute();
            fail("NuxeoDrive.CreateTestUser operation should not be callable by a non administrator.");
        } catch (Exception e) {
            // Expected
        }

        // Try to delete the nuxeoDriveTestUser as an unauthorized user =>
        // should fail
        try {
            unauthorizedSession.newRequest(NuxeoDriveDeleteTestUser.ID).execute();
            fail("NuxeoDrive.DeleteTestUser operation should not be callable by a non administrator.");
        } catch (Exception e) {
            // Expected
        }

        // Delete the nuxeoDriveTestUser as Administrator
        clientSession.newRequest(NuxeoDriveDeleteTestUser.ID).execute();
        assertNull(userManager.getPrincipal("nuxeoDriveTestUser"));
    }
}
