/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.fixtures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Tests the {@link FileSystemChangeFinder} on a repository with clustering enabled.
 *
 * @since 8.2
 */
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-clustering-enabled-repo-contrib.xml")
public class AuditChangeFinderClusteringEnabledTestSuite extends AbstractChangeFinderTestCase {

    private static final Logger log = LogManager.getLogger(AuditChangeFinderClusteringEnabledTestSuite.class);

    @Test
    public void testClusteringEnabled() throws Exception {
        List<FileSystemItemChange> changes;
        DocumentModel file1;
        DocumentModel file2;

        try {
            // No sync roots
            changes = getChanges();
            assertNotNull(changes);
            assertTrue(changes.isEmpty());

            log.trace("Register a sync root and create a document inside it");
            nuxeoDriveManager.registerSynchronizationRoot(session.getPrincipal(), folder1, session);
            file1 = session.createDocumentModel("/folder1", "file1", "File");
            file1.setPropertyValue("file:content", new StringBlob("The file content"));
            file1 = session.createDocument(file1);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        try {
            // NXP-22284: Cannot expect to have no changes if the clustering delay is not expired since waiting for
            // async completion has an unknown duration

            // Wait for (2 * clustering delay + 1 second) then check changes, expecting at least 2:
            // - documentCreated for file1
            // - rootRegistered for folder1
            // The documentCreated event for folder1 might have already been "swallowed" by the first call to
            // #getChanges() if the test initialization takes too much time, sometimes happens with an Oracle database
            Thread.sleep(3000);
            changes = getChanges();
            assertTrue(changes.size() >= 2);
            Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<>();
            expectedChanges.add(new SimpleFileSystemItemChange(file1.getId(), "documentCreated", "test",
                    "defaultFileSystemItemFactory#test#" + file1.getId(), "file1"));
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "rootRegistered", "test",
                    "defaultSyncRootFolderItemFactory#test#" + folder1.getId(), "folder1"));
            expectedChanges.add(new SimpleFileSystemItemChange(folder1.getId(), "documentCreated", "test",
                    "defaultSyncRootFolderItemFactory#test#" + folder1.getId(), "folder1"));
            assertTrue(CollectionUtils.isSubCollection(toSimpleFileSystemItemChanges(changes), expectedChanges));

            log.trace("Update existing document and create a new one");
            file1.setPropertyValue("dc:description", "Upated description");
            session.saveDocument(file1);
            file2 = session.createDocumentModel("/folder1", "file2", "File");
            file2.setPropertyValue("file:content", new StringBlob("The second file content"));
            file2 = session.createDocument(file2);
        } finally {
            commitAndWaitForAsyncCompletion();
        }

        // NXP-22284: Cannot expect to have no changes if the clustering delay is not expired since waiting for
        // async completion has an unknown duration

        // Wait for (2 * clustering delay + 1 second) then check changes, expecting 2:
        // - documentCreated for file2
        // - documentModified for file1
        Thread.sleep(3000);
        changes = getChanges();
        assertEquals(2, changes.size());
        Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<>();
        expectedChanges.add(new SimpleFileSystemItemChange(file2.getId(), "documentCreated", "test",
                "defaultFileSystemItemFactory#test#" + file2.getId(), "file2"));
        expectedChanges.add(new SimpleFileSystemItemChange(file1.getId(), "documentModified", "test",
                "defaultFileSystemItemFactory#test#" + file1.getId(), "file1"));
        assertTrue(CollectionUtils.isEqualCollection(expectedChanges, toSimpleFileSystemItemChanges(changes)));
    }

}
