/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.operations.test.NuxeoDriveSetActiveFactories;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;

/**
 * Tests the {@link NuxeoDriveSetActiveFactories} operation.
 * 
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(EmbeddedAutomationServerFeature.class)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.drive.operations", "org.nuxeo.runtime.reload" })
@Jetty(port = 18080)
public class TestActivateFactories {

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected Session clientSession;

    protected ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSetActiveFactories() throws Exception {

        // Check default factories
        checkDefaultProfile();

        // Check unknown profile
        Blob result = (Blob) clientSession.newRequest(NuxeoDriveSetActiveFactories.ID).set("profile", "unknown").execute();
        assertFalse(mapper.readValue(result.getStream(), Boolean.class));

        // Activate userworkspace factories
        result = (Blob) clientSession.newRequest(NuxeoDriveSetActiveFactories.ID).set("profile", "userworkspace").execute();
        assertTrue(mapper.readValue(result.getStream(), Boolean.class));
        checkUserworkspaceProfile();

        // Deactivate userworkspace factories
        result = (Blob) clientSession.newRequest(NuxeoDriveSetActiveFactories.ID).set("profile", "userworkspace").set(
                "enable", false).execute();
        assertTrue(mapper.readValue(result.getStream(), Boolean.class));
        checkDefaultProfile();

        // Activate permission factories
        result = (Blob) clientSession.newRequest(NuxeoDriveSetActiveFactories.ID).set("profile", "permission").execute();
        assertTrue(mapper.readValue(result.getStream(), Boolean.class));
        checkPermissionProfile();

        // Deactivate permission factories
        result = (Blob) clientSession.newRequest(NuxeoDriveSetActiveFactories.ID).set("profile", "permission").set(
                "enable", false).execute();
        assertTrue(mapper.readValue(result.getStream(), Boolean.class));
        checkDefaultProfile();

    }

    protected void checkDefaultProfile() throws ClientException {
        assertTrue(fileSystemItemAdapterService.getTopLevelFolderItemFactory().getName().endsWith(
                "DefaultTopLevelFolderItemFactory"));
        Set<String> activeFSItemFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(2, activeFSItemFactories.size());
        assertTrue(activeFSItemFactories.contains("defaultSyncRootFolderItemFactory"));
        assertTrue(activeFSItemFactories.contains("defaultFileSystemItemFactory"));
    }

    protected void checkUserworkspaceProfile() throws ClientException {
        assertTrue(fileSystemItemAdapterService.getTopLevelFolderItemFactory().getName().endsWith(
                "UserWorkspaceTopLevelFactory"));
        Set<String> activeFSItemFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(3, activeFSItemFactories.size());
        assertTrue(activeFSItemFactories.contains("userWorkspaceSyncRootParentFactory"));
        assertTrue(activeFSItemFactories.contains("userWorkspaceSyncRootFactory"));
        assertTrue(activeFSItemFactories.contains("defaultFileSystemItemFactory"));
    }

    protected void checkPermissionProfile() throws ClientException {
        assertTrue(fileSystemItemAdapterService.getTopLevelFolderItemFactory().getName().endsWith(
                "PermissionTopLevelFactory"));
        Set<String> activeFSItemFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(4, activeFSItemFactories.size());
        assertTrue(activeFSItemFactories.contains("userSyncRootParentFactory"));
        assertTrue(activeFSItemFactories.contains("permissionSyncRootFactory"));
        assertTrue(activeFSItemFactories.contains("sharedSyncRootParentFactory"));
        assertTrue(activeFSItemFactories.contains("defaultFileSystemItemFactory"));
    }

}
