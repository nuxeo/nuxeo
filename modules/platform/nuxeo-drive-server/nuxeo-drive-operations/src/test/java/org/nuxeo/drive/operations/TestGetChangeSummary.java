/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.fixtures.SimpleFileSystemItemChange;
import org.nuxeo.drive.service.FileSystemChangeSummary;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.FileSystemChangeSummaryImpl;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests the {@link NuxeoDriveGetChangeSummary} operation.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveAutomationFeature.class)
@Deploy("org.nuxeo.drive.operations:OSGI-INF/test-nuxeodrive-change-finder-contrib.xml")
public class TestGetChangeSummary {

    @Inject
    protected CoreSession session;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    @Inject
    protected HttpAutomationSession clientSession;

    protected long lastEventLogId;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    protected String lastSyncActiveRoots;

    @Before
    public void init() {

        lastEventLogId = 0;
        lastSyncActiveRoots = "";

        folder1 = session.createDocument(session.createDocumentModel("/", "folder1", "Folder"));
        folder2 = session.createDocument(session.createDocumentModel("/", "folder2", "Folder"));
    }

    @Test
    public void testGetChangesSummary() throws IOException, InterruptedException {

        // No sync roots => shouldn't find any changes
        FileSystemChangeSummary changeSummary = getChangeSummary();
        assertTrue(changeSummary.getFileSystemChanges().isEmpty());
        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());

        // Register sync roots and create 2 documents and 1 folder => should find 3 changes
        DocumentModel doc1;
        DocumentModel doc2;
        DocumentModel folder3;
        try {
            NuxeoPrincipal administrator = session.getPrincipal();
            nuxeoDriveManager.registerSynchronizationRoot(administrator, folder1, session);
            nuxeoDriveManager.registerSynchronizationRoot(administrator, folder2, session);

            doc1 = session.createDocumentModel("/folder1", "doc1", "File");
            doc1.setPropertyValue("file:content", new StringBlob("The content of file 1."));
            doc1 = session.createDocument(doc1);
            doc2 = session.createDocumentModel("/folder2", "doc2", "File");
            doc2.setPropertyValue("file:content", new StringBlob("The content of file 2."));
            doc2 = session.createDocument(doc2);
            folder3 = session.createDocumentModel("/folder2", "folder3", "Folder");
            folder3 = session.createDocument(folder3);

            session.save();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        changeSummary = getChangeSummary();
        List<FileSystemItemChange> docChanges = changeSummary.getFileSystemChanges();
        assertEquals(3, docChanges.size());

        Set<SimpleFileSystemItemChange> expectedChanges = new HashSet<>();
        expectedChanges.add(new SimpleFileSystemItemChange(folder3.getId(), "documentChanged", "test"));
        expectedChanges.add(new SimpleFileSystemItemChange(doc2.getId(), "documentChanged", "test"));
        expectedChanges.add(new SimpleFileSystemItemChange(doc1.getId(), "documentChanged", "test"));
        Set<SimpleFileSystemItemChange> changes = new HashSet<>();
        docChanges.forEach(docChange -> {
            changes.add(new SimpleFileSystemItemChange(docChange.getDocUuid(), docChange.getEventId(),
                    docChange.getRepositoryId()));
            assertNotNull(docChange.getFileSystemItem());
        });
        assertTrue(CollectionUtils.isEqualCollection(expectedChanges, changes));

        assertEquals(Boolean.FALSE, changeSummary.getHasTooManyChanges());
    }

    /**
     * Gets the changes summary for the user bound to the {@link #session} using the {@link NuxeoDriveGetChangeSummary}
     * automation operation and updates the {@link #lastEventLogId}.
     */
    protected FileSystemChangeSummary getChangeSummary() throws IOException, InterruptedException {
        // Wait 1 second as the mock change finder relies on steps of 1 second
        Thread.sleep(1000); // NOSONAR
        FileSystemChangeSummary changeSummary = clientSession.newRequest(NuxeoDriveGetChangeSummary.ID)
                                                             .set("lowerBound", lastEventLogId)
                                                             .set("lastSyncActiveRootDefinitions", lastSyncActiveRoots)
                                                             .executeReturning(FileSystemChangeSummaryImpl.class);
        assertNotNull(changeSummary);

        lastEventLogId = changeSummary.getUpperBound();
        lastSyncActiveRoots = changeSummary.getActiveSynchronizationRootDefinitions();
        return changeSummary;
    }

}
