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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.operations.test.NuxeoDriveSetActiveFactories;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.runtime.test.runner.ConsoleLogLevelThreshold;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;

/**
 * Tests the {@link NuxeoDriveSetActiveFactories} operation.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({ NuxeoDriveAutomationFeature.class, LogFeature.class, LogCaptureFeature.class })
public class TestActivateFactories {

    protected static final String PROFILE = "profile";

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected HttpAutomationSession clientSession;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    @ConsoleLogLevelThreshold("ERROR")
    public void testSetActiveFactories() throws IOException {

        // Check default factories
        checkDefaultProfile();

        // Check unknown profile
        boolean result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID) //
                                      .set(PROFILE, "unknown")
                                      .executeReturningBooleanEntity();
        assertFalse(result);
        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertEquals(1, caughtEvents.size());
        assertEquals("No active file system item factory contribution for profile 'unknown'.", caughtEvents.get(0));

        // Activate userworkspace factories
        result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID) //
                              .set(PROFILE, "userworkspace")
                              .executeReturningBooleanEntity();
        assertTrue(result);
        checkUserworkspaceProfile();

        // Deactivate userworkspace factories
        result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID)
                              .set(PROFILE, "userworkspace")
                              .set("enable", false)
                              .executeReturningBooleanEntity();
        assertTrue(result);
        checkDefaultProfile();

        // Activate permission factories
        result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID) //
                              .set(PROFILE, "permission")
                              .executeReturningBooleanEntity();
        assertTrue(result);
        checkPermissionProfile();

        // Deactivate permission factories
        result = clientSession.newRequest(NuxeoDriveSetActiveFactories.ID)
                              .set(PROFILE, "permission")
                              .set("enable", false)
                              .executeReturningBooleanEntity();
        assertTrue(result);
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
