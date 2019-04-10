/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.operations.test.NuxeoDriveSetActiveFactories;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

/**
 * Tests the {@link NuxeoDriveSetActiveFactories} operation.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveAutomationFeature.class)
@ServletContainer(port = 18080)
public class TestActivateFactories {

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected Session clientSession;

    @Test
    public void testSetActiveFactories() throws Exception {

        // Check default factories
        checkDefaultProfile();

        // Check unknown profile
        Object result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID).set("profile", "unknown").execute();
        assertFalse((Boolean) result);

        // Activate userworkspace factories
        result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID).set("profile", "userworkspace").execute();
        assertTrue((Boolean) result);
        checkUserworkspaceProfile();

        // Deactivate userworkspace factories
        result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID)
                              .set("profile", "userworkspace")
                              .set("enable", false)
                              .execute();
        assertTrue((Boolean) result);
        checkDefaultProfile();

        // Activate permission factories
        result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID).set("profile", "permission").execute();
        assertTrue((Boolean) result);
        checkPermissionProfile();

        // Deactivate permission factories
        result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID)
                              .set("profile", "permission")
                              .set("enable", false)
                              .execute();
        assertTrue((Boolean) result);
        checkDefaultProfile();

    }

    protected void checkDefaultProfile() {
        assertTrue(fileSystemItemAdapterService.getTopLevelFolderItemFactory()
                                               .getName()
                                               .endsWith("DefaultTopLevelFolderItemFactory"));
        Set<String> activeFSItemFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(3, activeFSItemFactories.size());
        assertTrue(activeFSItemFactories.contains("collectionSyncRootFolderItemFactory"));
        assertTrue(activeFSItemFactories.contains("defaultSyncRootFolderItemFactory"));
        assertTrue(activeFSItemFactories.contains("defaultFileSystemItemFactory"));
    }

    protected void checkUserworkspaceProfile() {
        assertTrue(fileSystemItemAdapterService.getTopLevelFolderItemFactory()
                                               .getName()
                                               .endsWith("UserWorkspaceTopLevelFactory"));
        Set<String> activeFSItemFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(4, activeFSItemFactories.size());
        assertTrue(activeFSItemFactories.contains("collectionSyncRootFolderItemFactory"));
        assertTrue(activeFSItemFactories.contains("userWorkspaceSyncRootParentFactory"));
        assertTrue(activeFSItemFactories.contains("userWorkspaceSyncRootFactory"));
        assertTrue(activeFSItemFactories.contains("defaultFileSystemItemFactory"));
    }

    protected void checkPermissionProfile() {
        assertTrue(fileSystemItemAdapterService.getTopLevelFolderItemFactory()
                                               .getName()
                                               .endsWith("PermissionTopLevelFactory"));
        Set<String> activeFSItemFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(5, activeFSItemFactories.size());
        assertTrue(activeFSItemFactories.contains("collectionSyncRootFolderItemFactory"));
        assertTrue(activeFSItemFactories.contains("userSyncRootParentFactory"));
        assertTrue(activeFSItemFactories.contains("permissionSyncRootFactory"));
        assertTrue(activeFSItemFactories.contains("sharedSyncRootParentFactory"));
        assertTrue(activeFSItemFactories.contains("defaultFileSystemItemFactory"));
    }

}
